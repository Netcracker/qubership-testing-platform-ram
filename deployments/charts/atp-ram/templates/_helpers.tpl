{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
{{ printf "- name: %s" $key }}
{{ printf "  value: \"%s\"" $val }}
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- range $keys }}
{{ printf "- name: %s" . }}
{{ printf "  valueFrom:" }}
{{ printf "    secretKeyRef:" }}
{{ printf "      name: %s-secrets" $.Values.SERVICE_NAME }}
{{ printf "      key: %s" . }}
{{- end }}
{{- end }}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ATP_ARCHIVE_BULK_DELETE: "{{ .Values.ATP_ARCHIVE_BULK_DELETE }}"
ATP_ARCHIVE_CRON_EXPRESSION: "{{ .Values.ATP_ARCHIVE_CRON_EXPRESSION }}"
ATP_ARCHIVE_JOB_NAME: "{{ .Values.ATP_ARCHIVE_JOB_NAME }}"
ATP_ARCHIVE_LIMIT_OF_DATA: "{{ .Values.ATP_ARCHIVE_LIMIT_OF_DATA }}"
ATP_HTTP_LOGGING_HEADERS_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_HEADERS_IGNORE }}"
ATP_HTTP_LOGGING_HEADERS: "{{ .Values.ATP_HTTP_LOGGING_HEADERS }}"
ATP_HTTP_LOGGING_URI_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_URI_IGNORE }}"
ATP_HTTP_LOGGING: "{{ .Values.ATP_HTTP_LOGGING }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_LAST_REVISION_COUNT: "{{ .Values.ATP_LAST_REVISION_COUNT }}"
ATP_LOGRECORD_STEP_FOR_RECALCULATING_TOPISSUES: "{{ .Values.ATP_LOGRECORD_STEP_FOR_RECALCULATING_TOPISSUES }}"
ATP_NOTIFICATION_MODE: "{{ .Values.ATP_NOTIFICATION_MODE }}"
ATP_RAM_URL: "{{ default .Values.IDENTITY_PROVIDER_URL .Values.ATP_RAM_URL }}"
ATP1_INTEGRATION_ENABLE: "{{ .Values.ATP1_INTEGRATION_ENABLE }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: "{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}"
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
BROWSER_MONITORING_LINK: "{{ .Values.BROWSER_MONITORING_LINK }}"
CATALOGUE_URL: "{{ .Values.CATALOGUE_URL }}"
CONNECTIONS_PER_HOST: "{{ .Values.CONNECTIONS_PER_HOST }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_HEALTH_CHECK_ENABLED: "{{ .Values.CONSUL_HEALTH_CHECK_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
RAM_EI_DB_ENABLE: "{{ .Values.RAM_EI_DB_ENABLE }}"
EI_GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
EI_GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
ENABLE_M2M: "{{ .Values.ENABLE_M2M }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
EXECUTION_REQUEST_CLEANUP_JOB_CRON: "{{ .Values.EXECUTION_REQUEST_CLEANUP_JOB_CRON }}"
EXPIRED_EXECUTION_REQUESTS_BATCH_SIZE: "{{ .Values.EXPIRED_EXECUTION_REQUESTS_BATCH_SIZE }}"
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_DATASETS_NAME: "{{ .Values.FEIGN_ATP_DATASETS_NAME }}"
FEIGN_ATP_DATASETS_ROUTE: "{{ .Values.FEIGN_ATP_DATASETS_ROUTE }}"
FEIGN_ATP_DATASETS_URL: "{{ .Values.FEIGN_ATP_DATASETS_URL }}"
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
FEIGN_ATP_ENVIRONMENTS_NAME: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_NAME }}"
FEIGN_ATP_ENVIRONMENTS_ROUTE: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_ROUTE }}"
FEIGN_ATP_ENVIRONMENTS_URL: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_URL }}"
FEIGN_ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.FEIGN_ATP_INTERNAL_GATEWAY_NAME }}"
FEIGN_ATP_MAILSENDER_NAME: "{{ .Values.FEIGN_ATP_MAILSENDER_NAME }}"
FEIGN_ATP_MAILSENDER_ROUTE: "{{ .Values.FEIGN_ATP_MAILSENDER_ROUTE }}"
FEIGN_ATP_MAILSENDER_URL: "{{ .Values.FEIGN_ATP_MAILSENDER_URL }}"
FEIGN_ATP_NOTIFICATION_NAME: "{{ .Values.FEIGN_ATP_NOTIFICATION_NAME }}"
FEIGN_ATP_NOTIFICATION_ROUTE: "{{ .Values.FEIGN_ATP_NOTIFICATION_ROUTE }}"
FEIGN_ATP_NOTIFICATION_URL: "{{ .Values.FEIGN_ATP_NOTIFICATION_URL }}"
FEIGN_ATP_ORCHESTRATOR_NAME: "{{ .Values.FEIGN_ATP_ORCHESTRATOR_NAME }}"
FEIGN_ATP_ORCHESTRATOR_ROUTE: "{{ .Values.FEIGN_ATP_ORCHESTRATOR_ROUTE }}"
FEIGN_ATP_ORCHESTRATOR_URL: "{{ .Values.FEIGN_ATP_ORCHESTRATOR_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
FILES_CLEANUP_JOB_ENABLE: "{{ .Values.FILES_CLEANUP_JOB_ENABLE }}"
FILES_EXPIRATION_DAYS_INTERVAL: "{{ .Values.FILES_EXPIRATION_DAYS_INTERVAL }}"
FILES_EXPIRATION_SCHEDULE_INTERVAL: "{{ .Values.FILES_EXPIRATION_SCHEDULE_INTERVAL }}"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.GRIDFS_DB "def" "atp-gridfs") }}"
GRIDFS_DB_PORT: "{{ .Values.GRIDFS_DB_PORT }}"
GRIDFS_DB_ADDR: "{{ .Values.GRIDFS_DB_ADDR }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
HAZELCAST_CLUSTER_NAME: "{{ .Values.HAZELCAST_CLUSTER_NAME }}"
HAZELCAST_ENABLE: "{{ .Values.HAZELCAST_ENABLE }}"
INTERNAL_JIRA_SYSTEM: "{{ .Values.INTERNAL_JIRA_SYSTEM }}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} {{ .Values.JAVA_OPTIONS }} {{ .Values.ADDITIONAL_JAVA_OPTIONS }}"
JAVERS_ENABLED: "{{ .Values.JAVERS_ENABLED }}"
KAFKA_ENABLE: "{{ .Values.KAFKA_ENABLE }}"
KAFKA_FDR_CONSUMER_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_FDR_CONSUMER_TOPIC_NAME "def" "fdr-links") }}"
KAFKA_FDR_ENABLE: "{{ .Values.KAFKA_FDR_ENABLE }}"
KAFKA_FDR_PRODUCER_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_FDR_PRODUCER_TOPIC_NAME "def" "fdr") }}"
KAFKA_GROUP_ID: "{{ .Values.KAFKA_GROUP_ID }}"
KAFKA_KEY_DESERIALIZER: "{{ .Values.KAFKA_KEY_DESERIALIZER }}"
KAFKA_MAILS_ENABLE: "{{ .Values.KAFKA_MAILS_ENABLE }}"
KAFKA_MAILS_MESSAGE_SIZE: "{{ .Values.KAFKA_MAILS_MESSAGE_SIZE }}"
KAFKA_MAILS_RESPONSE_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_MAILS_RESPONSE_TOPIC_NAME "def" "mail_responses") }}"
KAFKA_MAILS_RESPONSES_ENABLE: "{{ .Values.KAFKA_MAILS_RESPONSES_ENABLE }}"
KAFKA_MAILS_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_MAILS_TOPIC "def" "mails") }}"
KAFKA_NOTIFICATION_TOPIC_PARTITIONS: "{{ .Values.KAFKA_NOTIFICATION_TOPIC_PARTITIONS }}"
KAFKA_NOTIFICATION_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_NOTIFICATION_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_NOTIFICATION_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_NOTIFICATION_TOPIC "def" "notification_topic") }}"
KAFKA_PROJECT_EVENT_CONSUMER_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_PROJECT_EVENT_CONSUMER_TOPIC_NAME "def" "catalog_notification_topic") }}"
KAFKA_PROJECT_EVENT_ENABLE: "{{ .Values.KAFKA_PROJECT_EVENT_ENABLE }}"
KAFKA_RCA_CONSUMER_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_RCA_CONSUMER_TOPIC_NAME "def" "rca") }}"
KAFKA_RCA_ENABLE: "{{ .Values.KAFKA_RCA_ENABLE }}"
KAFKA_REPORTING_SERVERS: "{{ .Values.KAFKA_REPORTING_SERVERS }}"
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service_entities") }}"
KAFKA_TEST_PLAN_EVENT_ENABLE: "{{ .Values.KAFKA_TEST_PLAN_EVENT_ENABLE }}"
KAFKA_TEST_PLAN_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_TEST_PLAN_TOPIC_NAME "def" "catalog_test_plan_topic") }}"
KAFKA_TRUSTED_PACKAGES: "{{ .Values.KAFKA_TRUSTED_PACKAGES }}"
KAFKA_TYPE_MAPPING: "{{ .Values.KAFKA_TYPE_MAPPING }}"
KAFKA_VALUE_DESERIALIZER: "{{ .Values.KAFKA_VALUE_DESERIALIZER }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
LIMIT_TESTRESULTS_CATALOG_DASHBOARD: "{{ .Values.LIMIT_TESTRESULTS_CATALOG_DASHBOARD }}"
LOCALE_RESOLVER: "{{ .Values.LOCALE_RESOLVER }}"
LOG_LEVEL: "{{ .Values.LOG_LEVEL }}"
LOG_RECORDS_CONTEXT_VARIABLES_DATE_EXPIRE_SECONDS: "{{ .Values.LOG_RECORDS_CONTEXT_VARIABLES_DATE_EXPIRE_SECONDS }}"
LOG_RECORDS_DATE_EXPIRE_SECONDS: "{{ .Values.LOG_RECORDS_DATE_EXPIRE_SECONDS }}"
MAIL_HOST: "{{ .Values.MAIL_HOST }}"
MAIL_SENDER_URL: "{{ .Values.MAIL_SENDER_URL }}"
MAIL_SSL: "{{ .Values.MAIL_SSL }}"
MAX_CONNECTION_IDLE_TIME: "{{ .Values.MAX_CONNECTION_IDLE_TIME }}"
MAX_FILE_SIZE: "{{ .Values.MAX_FILE_SIZE }}"
MAX_RAM: "{{ .Values.MAX_RAM }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MIN_CONNECTIONS_PER_HOST: "{{ .Values.MIN_CONNECTIONS_PER_HOST }}"
MONGO_DB_ADDR: "{{ .Values.MONGO_DB_ADDR }}"
MONGO_DB_PORT: "{{ .Values.MONGO_DB_PORT }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
RAM_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.RAM_DB) }}"
REGEXP_TIMEOUT: "{{ .Values.REGEXP_TIMEOUT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
SCHEDULER_RATE_ER: "{{ default "1800000" .Values.SCHEDULER_RATE_ER }}"
SCHEDULER_RATE_TR: "{{ default "1800000" .Values.SCHEDULER_RATE_TR }}"
SERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SSL_CERTIFICATE_PATH: "{{ .Values.SSL_CERTIFICATE_PATH }}"
SSL_VERIFY: "{{ .Values.SSL_VERIFY }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
TERMINATE_TIMEOUT_IN_MINUTES: "{{ .Values.TERMINATE_TIMEOUT_IN_MINUTES }}"
UNDERTOW_THREADS_IO: "{{ .Values.UNDERTOW_THREADS_IO }}"
UNDERTOW_THREADS_WORKER: "{{ .Values.UNDERTOW_THREADS_WORKER }}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
{{- end }}


{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
GRIDFS_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.GRIDFS_DB_PASSWORD "def" "atp-gridfs") }}"
GRIDFS_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.GRIDFS_DB_USER "def" "atp-gridfs") }}"
KEYCLOAK_CLIENT_NAME: "{{ default "ram" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "b33ede0f-2834-4f43-aaec-ee8515e21f61" .Values.KEYCLOAK_SECRET }}"
RAM_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.RAM_DB_PASSWORD) }}"
RAM_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.RAM_DB_USER) }}"
{{- end }}

{{- define "env.deploy" }}
ei_gridfs_pass: "{{ .Values.ei_gridfs_pass }}"
ei_gridfs_user: "{{ .Values.ei_gridfs_user }}"
gridfs_pass: "{{ .Values.gridfs_pass }}"
gridfs_user: "{{ .Values.gridfs_user }}"
mongo_pass: "{{ .Values.mongo_pass }}"
mongo_user: "{{ .Values.mongo_user }}"
{{- end }}
