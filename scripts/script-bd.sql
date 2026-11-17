-- Azure SQL / SQL Server. Idempotent initial DDL; not a migration mechanism.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
-- Instant DDL matches Hibernate SQLServerDialect metadata (datetimeoffset); UTC instants.
IF OBJECT_ID('dbo.usuarios','U') IS NULL
CREATE TABLE dbo.usuarios (
 id BIGINT IDENTITY(1,1) PRIMARY KEY,
 nome VARCHAR(120) NOT NULL,
 email VARCHAR(160) NOT NULL UNIQUE,
 senha VARCHAR(100) NOT NULL,
 papel VARCHAR(20) NOT NULL CHECK(papel IN ('CUIDADOR','GESTOR'))
);
IF OBJECT_ID('dbo.pacientes','U') IS NULL
CREATE TABLE dbo.pacientes (
 id BIGINT IDENTITY(1,1) PRIMARY KEY,
 nome VARCHAR(120) NOT NULL,
 endereco VARCHAR(200) NOT NULL,
 telefone VARCHAR(30) NOT NULL,
 cuidador_id BIGINT NOT NULL REFERENCES dbo.usuarios(id),
 plano_contingencia VARCHAR(6000) NULL
);
IF OBJECT_ID('dbo.aparelhos','U') IS NULL
CREATE TABLE dbo.aparelhos (
 id BIGINT IDENTITY(1,1) PRIMARY KEY,
 paciente_id BIGINT NOT NULL REFERENCES dbo.pacientes(id),
 modelo VARCHAR(120) NOT NULL,
 tipo VARCHAR(30) NOT NULL CHECK(tipo IN ('VENTILADOR','CONCENTRADOR','BOMBA','OUTRO')),
 potencia_watts INT NOT NULL CHECK(potencia_watts>0),
 autonomia_minutos INT NOT NULL CHECK(autonomia_minutos>=0)
);
IF OBJECT_ID('dbo.ocorrencias','U') IS NULL
CREATE TABLE dbo.ocorrencias (
 id BIGINT IDENTITY(1,1) PRIMARY KEY,
 versao BIGINT NULL,
 paciente_id BIGINT NOT NULL REFERENCES dbo.pacientes(id),
 solicitante_id BIGINT NOT NULL REFERENCES dbo.usuarios(id),
 chave VARCHAR(64) NOT NULL,
 inicio DATETIMEOFFSET(6) NOT NULL,
 autonomia_inicial INT NOT NULL,
 desempate INT NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('ABERTA','ENCERRADA')),
 observacao VARCHAR(400) NOT NULL,
 evento_pendente BIT NOT NULL DEFAULT 1,
 evento_recebido BIT NOT NULL DEFAULT 0,
 encerramento DATETIMEOFFSET(6) NULL,
 CONSTRAINT uq_ocorrencia_chave UNIQUE(solicitante_id,chave)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_paciente_cuidador')
 CREATE INDEX ix_paciente_cuidador ON dbo.pacientes(cuidador_id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_aparelho_paciente')
 CREATE INDEX ix_aparelho_paciente ON dbo.aparelhos(paciente_id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_ocorrencia_status')
 CREATE INDEX ix_ocorrencia_status ON dbo.ocorrencias(status,paciente_id);
-- At most one active occurrence per patient, including concurrent requests.
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='uq_ocorrencia_aberta')
 CREATE UNIQUE INDEX uq_ocorrencia_aberta ON dbo.ocorrencias(paciente_id) WHERE status='ABERTA';
