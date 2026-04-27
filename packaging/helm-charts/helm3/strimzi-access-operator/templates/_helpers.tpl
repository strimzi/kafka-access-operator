{{/*
Generate the watch namespaces list for ACCESS_WATCHED_NAMESPACES environment variable
*/}}
{{- define "strimzi.accessWatchNamespacesList" -}}
{{- if .Values.accessWatchNamespaces -}}
{{- if kindIs "slice" .Values.accessWatchNamespaces -}}
{{- join "," .Values.accessWatchNamespaces -}}
{{- else if eq .Values.accessWatchNamespaces "*" -}}
*
{{- else -}}
{{- .Values.accessWatchNamespaces -}}
{{- end -}}
{{- else -}}
{{- .Release.Namespace -}}
{{- end -}}
{{- end -}}

{{/*
Generate the watch namespaces list for KAFKA_WATCHED_NAMESPACES environment variable
*/}}
{{- define "strimzi.kafkaWatchNamespacesList" -}}
{{- if .Values.kafkaWatchNamespaces -}}
{{- if kindIs "slice" .Values.kafkaWatchNamespaces -}}
{{- join "," .Values.kafkaWatchNamespaces -}}
{{- else if eq .Values.kafkaWatchNamespaces "*" -}}
*
{{- else -}}
{{- .Values.kafkaWatchNamespaces -}}
{{- end -}}
{{- else -}}
{{- .Release.Namespace -}}
{{- end -}}
{{- end -}}
