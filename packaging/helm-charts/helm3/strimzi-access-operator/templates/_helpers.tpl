{{/*
Generate the watch namespaces list for STRIMZI_NAMESPACE environment variable
*/}}
{{- define "strimzi.watchNamespacesList" -}}
{{- if .Values.watchNamespaces }}
{{- if eq .Values.watchNamespaces "*" }}
*
{{- else if kindIs "slice" .Values.watchNamespaces }}
{{- join "," .Values.watchNamespaces }}
{{- else }}
{{- .Values.watchNamespaces }}
{{- end }}
{{- else }}
{{- .Release.Namespace }}
{{- end }}
{{- end -}}
