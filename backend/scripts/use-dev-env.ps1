$env:JAVA_HOME = 'E:\tools\jdk-21.0.10+7'
$env:PGROOT = 'E:\tools\postgresql-18.3\pgsql'
$env:Path = 'E:\tools\jdk-21.0.10+7\bin;E:\tools\postgresql-18.3\pgsql\bin;' + $env:Path

$env:MDM_DB_URL = 'jdbc:postgresql://localhost:5432/mdm_dev'
$env:MDM_DB_USERNAME = 'postgres'
$env:MDM_DB_PASSWORD = 'postgres'
$env:MDM_REDIS_HOST = 'localhost'
$env:MDM_REDIS_PORT = '6379'
$env:MDM_REDIS_PASSWORD = 'root'
$env:MDM_STORAGE_ROOT = 'E:/workspace/mdm/backend/storage'
$env:MDM_JWT_SECRET = 'mdm-dev-secret-key-2026-with-32-chars'
$env:MDM_SWAGGER_ENABLED = 'true'
