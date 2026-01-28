# Strimzi Access Operator Installation

This directory contains the Kubernetes manifests for deploying the Strimzi Access Operator. The operator can be deployed in two modes:

- **Cluster-wide mode**: Watches KafkaAccess resources in all namespaces
- **Namespace-scoped mode**: Watches KafkaAccess resources only in specific namespaces

## Prerequisites

Before deploying the Access Operator, you must have the Strimzi `Kafka` and `KafkaUser` CRDs installed in your cluster. You can:
- Get these from the [Strimzi GitHub repository](https://github.com/strimzi/strimzi-kafka-operator/tree/main/install/cluster-operator)
- Use the [Strimzi quickstart guide](https://strimzi.io/quickstarts/) to deploy the Strimzi cluster operator and a Kafka instance

## Deployment Modes

### Cluster-Wide Deployment

In cluster-wide mode, the operator watches for KafkaAccess resources across all namespaces in the cluster.

**When to use:**
- You want a single operator instance to manage KafkaAccess resources across the entire cluster
- You have cluster-admin privileges
- You want simplified management with one operator for all teams/namespaces

**Installation:**

**Apply all manifests:**
   ```bash
   kubectl apply -f install/000-Namespace.yaml
   kubectl apply -f install/010-ServiceAccount.yaml
   kubectl apply -f install/020-ClusterRole.yaml
   kubectl apply -f install/030-ClusterRoleBinding.yaml
   kubectl apply -f install/040-Crd-kafkaaccess.yaml
   kubectl apply -f install/050-Deployment.yaml
   ```

   Or apply the entire directory at once:
   ```bash
   kubectl apply -f install/
   ```
   Note: This will also apply the RoleBinding files (060, 061), which are unnecessary in cluster-wide mode.

**RBAC:** Uses `ClusterRoleBinding` to grant cluster-wide permissions.

---

### Namespace-Scoped Deployment

In namespace-scoped mode, the operator watches for KafkaAccess resources only in specified namespaces.

**When to use:**
- You want to limit the operator's scope to specific namespaces
- You have security requirements that prevent cluster-wide access
- You want to run multiple operator instances watching different namespace sets
- You follow multi-tenancy patterns where teams manage their own namespaces

**Installation:**

1. **Modify the Deployment** to set the `STRIMZI_NAMESPACE` environment variable:

   Edit `050-Deployment.yaml` to list your target namespaces:
   ```yaml
   env:
     - name: STRIMZI_NAMESPACE
       value: "myproject,namespace2,namespace3"  # Comma-separated list
   ```

2. **Apply base manifests one by one** (excluding ClusterRoleBinding):
   ```bash
   kubectl apply -f install/000-Namespace.yaml
   kubectl apply -f install/010-ServiceAccount.yaml
   kubectl apply -f install/020-ClusterRole.yaml
   # SKIP 030-ClusterRoleBinding.yaml
   kubectl apply -f install/040-Crd-kafkaaccess.yaml
   kubectl apply -f install/050-Deployment.yaml
   ```

3. **Create RoleBindings for each watched namespace:**

   For each namespace listed in `STRIMZI_NAMESPACE`, create a RoleBinding. You can use `060-RoleBinding-myproject.yaml` and `061-RoleBinding-namespace2.yaml` as templates.

   **Example for namespace `my-namespace`:**
   ```yaml
   apiVersion: rbac.authorization.k8s.io/v1
   kind: RoleBinding
   metadata:
     name: strimzi-access-operator
     namespace: myproject  # Target namespace
     labels:
       app: strimzi-access-operator
   subjects:
     - kind: ServiceAccount
       name: strimzi-access-operator
       namespace: strimzi-access-operator  # Operator's namespace
   roleRef:
     kind: ClusterRole
     name: strimzi-access-operator
     apiGroup: rbac.authorization.k8s.io
   ```

   Apply the RoleBinding:
   ```bash
   kubectl apply -f rolebinding-my-namespace.yaml
   ```

   Repeat for each namespace you want the operator to watch.

**RBAC:** Uses `RoleBinding` in each watched namespace to grant namespace-scoped permissions only.

---

## Understanding the RBAC Setup

### ClusterRole (020-ClusterRole.yaml)
Defines the permissions needed by the operator. Contains only namespace-scoped resources:
- `kafkaaccesses` (Access Operator CRD)
- `kafkas`, `kafkausers` (Strimzi CRDs)
- `secrets` (to create binding secrets)

### Cluster-Wide Mode RBAC
- **ClusterRoleBinding** (030): Binds the ClusterRole to the ServiceAccount cluster-wide
- The operator can access resources in **all namespaces**

### Namespace-Scoped Mode RBAC
- **RoleBinding** (060, 061, etc.): Binds the ClusterRole to the ServiceAccount per-namespace
- The operator can **only** access resources in namespaces where a RoleBinding exists
- More secure and follows the principle of least privilege

### Why RoleBinding can reference a ClusterRole
This is a standard Kubernetes pattern that allows you to:
1. Define permissions once in a ClusterRole
2. Grant those permissions at either cluster or namespace scope
3. Reuse the same ClusterRole definition for both deployment modes

## Files Overview

| File | Purpose | Cluster-Wide | Namespace-Scoped |
|------|---------|--------------|------------------|
| `000-Namespace.yaml` | Creates `strimzi-access-operator` namespace | Required | Required |
| `010-ServiceAccount.yaml` | ServiceAccount for the operator | Required | Required |
| `020-ClusterRole.yaml` | Defines operator permissions | Required | Required |
| `030-ClusterRoleBinding.yaml` | Grants cluster-wide permissions | **Required** | **DO NOT APPLY** |
| `040-Crd-kafkaaccess.yaml` | KafkaAccess CRD | Required | Required |
| `050-Deployment.yaml` | Operator deployment | Required (no STRIMZI_NAMESPACE) | Required (with STRIMZI_NAMESPACE) |
| `060-RoleBinding.yaml` | Example RoleBinding for `myproject` | Optional (not used) | **Required** (customize) |
| `061-RoleBinding-testNamespace.yaml` | Example RoleBinding for `testNamespace` | Optional (not used) | **Required** (customize) |

## Verification

After deployment, verify the operator is running:

```bash
kubectl get pods -n strimzi-access-operator
```

Check the operator logs to confirm namespace configuration:

```bash
kubectl logs -n strimzi-access-operator deployment/strimzi-access-operator
```

For cluster-wide mode, you should see:
```
Kafka Access operator starting
```

For namespace-scoped mode with specific namespaces, the operator will only reconcile KafkaAccess resources in the configured namespaces.

## Example Usage

Create a KafkaAccess resource in one of the watched namespaces:

```yaml
apiVersion: access.strimzi.io/v1alpha1
kind: KafkaAccess
metadata:
  name: my-kafka-access
  namespace: myproject  # Must be in a watched namespace
spec:
  kafka:
    name: my-cluster
    namespace: kafka-namespace
```

Apply it:
```bash
kubectl apply -f kafka-access.yaml
```

The operator will create a Secret with connection details:
```bash
kubectl get secret my-kafka-access -n myproject
```

## Troubleshooting

### Operator can't see KafkaAccess resources
- **Cluster-wide mode**: Verify `030-ClusterRoleBinding.yaml` is applied
- **Namespace-scoped mode**:
  - Verify `STRIMZI_NAMESPACE` is set correctly in the Deployment
  - Verify RoleBinding exists in the target namespace
  - Check RoleBinding references the correct ServiceAccount namespace

### Permission errors in logs
- Verify the ClusterRole has all necessary permissions
- Ensure the RoleBinding/ClusterRoleBinding is correctly applied
- Check that the ServiceAccount name matches in all resources

### Operator not reconciling resources
- Check operator logs: `kubectl logs -n strimzi-access-operator deployment/strimzi-access-operator`
- Verify Strimzi CRDs (Kafka, KafkaUser) are installed
- Ensure the referenced Kafka cluster exists and is in a watched namespace
