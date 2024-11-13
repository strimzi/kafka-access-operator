#!/usr/bin/env bash
set -xe

rm -rf ~/.kube

KIND_VERSION=${KIND_VERSION:-"v0.25.0"}
# To properly upgrade Kind version check the releases in github https://github.com/kubernetes-sigs/kind/releases and use proper image based on Kind version
KIND_NODE_IMAGE=${KIND_NODE_IMAGE:-"kindest/node:v1.26.15@sha256:c79602a44b4056d7e48dc20f7504350f1e87530fe953428b792def00bc1076dd"}
COPY_DOCKER_LOGIN=${COPY_DOCKER_LOGIN:-"false"}

KIND_CLUSTER_NAME="kind-cluster"

# note that IPv6 is only supported on kind (i.e., minikube does not support it). Also we assume that when you set this flag
# to true then you meet requirements (i.) net.ipv6.conf.all.disable_ipv6 = 0 (ii. you have installed CNI supporting IPv6)
IP_FAMILY=${IP_FAMILY:-"ipv4"}

ARCH=$1
if [ -z "$ARCH" ]; then
    ARCH="amd64"
fi

function install_kubectl {
    if [ "${TEST_KUBECTL_VERSION:-latest}" = "latest" ]; then
        TEST_KUBECTL_VERSION=$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)
    fi
    curl -Lo kubectl https://storage.googleapis.com/kubernetes-release/release/${TEST_KUBECTL_VERSION}/bin/linux/${ARCH}/kubectl && chmod +x kubectl
    sudo cp kubectl /usr/local/bin
}

function label_node {
	# It should work for all clusters
	for nodeName in $(kubectl get nodes -o custom-columns=:.metadata.name --no-headers);
	do
		echo ${nodeName};
		kubectl label node ${nodeName} rack-key=zone;
	done
}

function install_kubernetes_provisioner {

    KIND_URL=https://github.com/kubernetes-sigs/kind/releases/download/${KIND_VERSION}/kind-linux-${ARCH}

    curl -Lo kind ${KIND_URL} && chmod +x kind
    sudo cp kind /usr/local/bin
}

function create_cluster_role_binding_admin {
    kubectl create clusterrolebinding add-on-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:default
}

: '
@brief: Set up Kubernetes configuration directory and file.
@note: Ensures $HOME/.kube directory and $HOME/.kube/config file exist.
'
function setup_kube_directory {
    mkdir $HOME/.kube || true
    touch $HOME/.kube/config
}

: '
@brief: Add Docker Hub credentials to Kubernetes node.
@param $1: Container name/ID.
@global: COPY_DOCKER_LOGIN - If "true", copies credentials.
@note: Uses hosts $HOME/.docker/config.json.
'
function add_docker_hub_credentials_to_kubernetes {
    # Add Docker hub credentials to Minikube
    if [ "$COPY_DOCKER_LOGIN" = "true" ]
    then
      for node in $(kind get nodes --name "${KIND_CLUSTER_NAME}"); do
        if [[ "$node" != *"external-load-balancer"* ]];
        then
          # the -oname format is kind/name (so node/name) we just want name
          node_name=${node#node/}
          # copy the config to where kubelet will look
          docker cp "$HOME/.docker/config.json" "${node_name}:/var/lib/kubelet/config.json"
          # restart kubelet to pick up the config
          docker exec "${node_name}" systemctl restart kubelet.service
        fi
      done
    fi
}

: '
@brief: Update Docker daemon configuration and restart service.
@param $1: JSON string for Docker daemon configuration.
@note: Requires sudo permissions.
'
function updateDockerDaemonConfiguration() {
    # We need to add such host to insecure-registry (as localhost is default)
    echo $1 | sudo tee /etc/docker/daemon.json
    # we need to restart docker service to propagate configuration
    sudo systemctl restart docker
}

: '
@brief: Increases the inotify user watches and user instances limits on a Linux system.
@param: None.
@global: None.
@note: Inotify is a Linux subsystem used for file system event notifications. This function
       helps adjust the limits for applications or services that monitor a large number
       of files or directories.
       This is specifically needed for multi-node control plane cluster
       https://github.com/kubernetes-sigs/kind/issues/2744#issuecomment-1127808069
'
function adjust_inotify_limits {
    # Increase the inotify user watches limit
    echo "Setting fs.inotify.max_user_watches to 655360..."
    echo fs.inotify.max_user_watches=655360 | sudo tee -a /etc/sysctl.conf

    # Increase the inotify user instances limit
    echo "Setting fs.inotify.max_user_instances to 1280..."
    echo fs.inotify.max_user_instances=1280 | sudo tee -a /etc/sysctl.conf

    # Reload the system configuration settings
    echo "Reloading sysctl settings..."
    sudo sysctl -p

    echo "Inotify limits adjusted successfully."
}

setup_kube_directory
install_kubectl
install_kubernetes_provisioner
adjust_inotify_limits

reg_name='kind-registry'
reg_port='5001'

if [[ "$IP_FAMILY" = "ipv4" || "$IP_FAMILY" = "dual" ]]; then
    hostname=$(hostname --ip-address | grep -oE '\b([0-9]{1,3}\.){3}[0-9]{1,3}\b' | awk '$1 != "127.0.0.1" { print $1 }' | head -1)

    # update insecure registries
    updateDockerDaemonConfiguration "{ \"insecure-registries\" : [\"${hostname}:${reg_port}\"] }"

    # Create kind cluster with containerd registry config dir enabled
    # TODO: kind will eventually enable this by default and this patch will
    # be unnecessary.
    #
    # See:
    # https://github.com/kubernetes-sigs/kind/issues/2875
    # https://github.com/containerd/containerd/blob/main/docs/cri/config.md#registry-configuration
    # See: https://github.com/containerd/containerd/blob/main/docs/hosts.md
    cat <<EOF | kind create cluster --image "${KIND_NODE_IMAGE}" --config=-
    kind: Cluster
    apiVersion: kind.x-k8s.io/v1alpha4
    nodes:
    - role: control-plane
    - role: control-plane
    - role: control-plane
    - role: worker
    - role: worker
    - role: worker
    name: $KIND_CLUSTER_NAME
    containerdConfigPatches:
    - |-
     [plugins."io.containerd.grpc.v1.cri".registry]
       config_path = "/etc/containerd/certs.d"
    networking:
       ipFamily: $IP_FAMILY
EOF
    # run local container registry
    if [ "$(docker inspect -f '{{.State.Running}}' "${reg_name}" 2>/dev/null || true)" != 'true' ]; then
        docker run \
          -d --restart=always -p "${hostname}:${reg_port}:5000" --name "${reg_name}" \
          registry:2
    fi

    # Add the registry config to the nodes
    #
    # This is necessary because localhost resolves to loopback addresses that are
    # network-namespace local.
    # In other words: localhost in the container is not localhost on the host.
    #
    # We want a consistent name that works from both ends, so we tell containerd to
    # alias localhost:${reg_port} to the registry container when pulling images
    # note: kind get nodes (default name `kind` and with specifying new name we have to use --name <cluster-name>
    # See https://kind.sigs.k8s.io/docs/user/local-registry/
    REGISTRY_DIR="/etc/containerd/certs.d/${hostname}:${reg_port}"

    for node in $(kind get nodes --name "${KIND_CLUSTER_NAME}"); do
        echo "Executing command in node:${node}"
        docker exec "${node}" mkdir -p "${REGISTRY_DIR}"
        cat <<EOF | docker exec -i "${node}" cp /dev/stdin "${REGISTRY_DIR}/hosts.toml"
    [host."http://${reg_name}:5000"]
EOF
    done

elif [[ "$IP_FAMILY" = "ipv6" ]]; then
    # for ipv6 configuration
    ula_fixed_ipv6="fd01:2345:6789"
    registry_dns="myregistry.local"

    # manually assign an IPv6 address to eth0 interface
    sudo ip -6 addr add "${ula_fixed_ipv6}"::1/64 dev eth0

     # use ULA (i.e., Unique Local Address), which offers a similar "private" scope as link-local
    # but without the interface dependency and some of the other challenges of link-local addresses.
    # (link-local starts as fe80::) but we will use ULA fd01
    updateDockerDaemonConfiguration "{
        \"insecure-registries\" : [\"[${ula_fixed_ipv6}::1]:${reg_port}\", \"${registry_dns}:${reg_port}\"],
        \"experimental\": true,
        \"ip6tables\": true,
        \"fixed-cidr-v6\": \"${ula_fixed_ipv6}::/80\"
    }"

    cat <<EOF | kind create cluster --image "${KIND_NODE_IMAGE}" --config=-
    kind: Cluster
    apiVersion: kind.x-k8s.io/v1alpha4
    nodes:
    - role: control-plane
    - role: control-plane
    - role: control-plane
    - role: worker
    - role: worker
    - role: worker
    name: $KIND_CLUSTER_NAME
    containerdConfigPatches:
    - |-
      [plugins."io.containerd.grpc.v1.cri".registry.mirrors."myregistry.local:5001"]
          endpoint = ["http://myregistry.local:5001"]
    networking:
        ipFamily: $IP_FAMILY
EOF
    # run local container registry
    if [ "$(docker inspect -f '{{.State.Running}}' "${reg_name}" 2>/dev/null || true)" != 'true' ]; then
        docker run \
          -d --restart=always -p "[${ula_fixed_ipv6}::1]:${reg_port}:5000" --name "${reg_name}" \
          registry:2
    fi
    # we need to also make a DNS record for docker tag because it seems that such version does not support []:: format
    echo "${ula_fixed_ipv6}::1    ${registry_dns}" >> /etc/hosts

    # note: kind get nodes (default name `kind` and with specifying new name we have to use --name <cluster-name>
    # See https://kind.sigs.k8s.io/docs/user/local-registry/
    for node in $(kind get nodes --name "${KIND_CLUSTER_NAME}"); do
        echo "Executing command in node:${node}"
        cat <<EOF | docker exec -i "${node}" cp /dev/stdin "/etc/hosts"
${ula_fixed_ipv6}::1    ${registry_dns}
EOF
    done
fi

# Connect the registry to the cluster network if not already connected
# This allows kind to bootstrap the network but ensures they're on the same network
if [ "$(docker inspect -f='{{json .NetworkSettings.Networks.kind}}' "${reg_name}")" = 'null' ]; then
  docker network connect "kind" "${reg_name}"
fi

add_docker_hub_credentials_to_kubernetes

create_cluster_role_binding_admin
label_node
