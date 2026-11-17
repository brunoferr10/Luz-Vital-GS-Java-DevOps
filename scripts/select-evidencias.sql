-- Run after EACH POST / PUT / DELETE while recording the continuous video.
-- Never show password hashes, keys or tokens on screen.
SELECT id,nome,email,papel FROM dbo.usuarios ORDER BY id;
SELECT id,nome,endereco,telefone,cuidador_id,plano_contingencia FROM dbo.pacientes ORDER BY id;
SELECT id,paciente_id,modelo,tipo,potencia_watts,autonomia_minutos FROM dbo.aparelhos ORDER BY id;
SELECT id,paciente_id,inicio,status,observacao,autonomia_inicial,evento_pendente,evento_recebido,encerramento FROM dbo.ocorrencias ORDER BY id;
