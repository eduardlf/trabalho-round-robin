# Trabalho Final de Sistemas Operacionais

## Simulador de Escalonamento Round Robin com Feedback

### Grupo 5

* CARLOS EDUARDO SEIDL
* EDUARDO LENHARDT FELTES
* LUCAS FREITAS JANZEN
* RAFAEL FÜHR MARONEZ

---

## Linguagem Utilizada

* Java

---

## Sobre o Projeto

Este projeto implementa um simulador do algoritmo de escalonamento **Round Robin com Feedback**, desenvolvido para a disciplina de Sistemas Operacionais.

O objetivo do simulador é representar de forma simplificada o funcionamento de um sistema operacional multitarefa, demonstrando conceitos fundamentais como:

* Escalonamento de processos;
* Preempção por quantum;
* Filas de prioridade;
* Operações de Entrada e Saída (I/O);
* Estados de processos;
* Utilização da CPU;
* Métricas de desempenho.

A implementação segue a orientação do trabalho, utilizando uma abordagem predominantemente procedural, concentrando toda a lógica em uma única classe principal (`Round_robin.java`) e utilizando apenas estruturas simples e `record` para representar o PCB (Process Control Block) e eventos de I/O.

---

# Algoritmo Implementado

O simulador utiliza uma estratégia de **Round Robin com Feedback**, baseada em duas filas de processos prontos:

## Fila de Alta Prioridade

Todo processo recém-criado entra inicialmente nesta fila.

A CPU sempre prioriza a execução dos processos presentes nesta fila.

## Fila de Baixa Prioridade

Processos que consomem todo o quantum sem finalizar sua execução são movidos para esta fila.

A fila de baixa prioridade somente é utilizada quando não existem processos disponíveis na fila de alta prioridade.

---

# Tratamento de Entrada e Saída (I/O)

O simulador possui suporte aos seguintes dispositivos:

* Disco
* Fita Magnética
* Impressora

Quando um processo solicita uma operação de I/O:

1. O processo é removido da CPU.
2. Seu estado muda para **BLOQUEADO**.
3. Um evento é inserido na fila de I/O.
4. Após o término da operação, o processo retorna para uma fila de execução.

### Política de Retorno

| Dispositivo    | Fila de Retorno  |
| -------------- | ---------------- |
| Disco          | Baixa Prioridade |
| Fita Magnética | Alta Prioridade  |
| Impressora     | Alta Prioridade  |

---

# Estrutura Geral do Código

O arquivo `Main.java` está organizado em seções para facilitar a leitura e manutenção.

## Configurações

Contém constantes utilizadas pela simulação:

* Quantum
* Quantidade de processos
* Seed do gerador aleatório
* Tempos de I/O
* Intervalos de geração

## Enumerações

Representam os estados e classificações utilizadas pelo simulador:

* Status
* Prioridade
* Dispositivo

## Records

### Processo

Representa um PCB simplificado contendo:

* PID
* PPID
* Tempo de chegada
* Tempo total de CPU
* Tempo restante
* Informações de I/O
* Prioridade atual
* Estado atual
* Métricas de execução

### EventoIO

Representa um evento pendente de entrada e saída.

## Filas

O simulador utiliza:

* Fila Alta
* Fila Baixa
* Fila de I/O

---

# Funcionamento da Simulação

A execução ocorre em unidades discretas de tempo.

A cada ciclo da simulação são executadas as seguintes etapas:

1. Verificação de novos processos.
2. Atualização da fila de I/O.
3. Atualização do tempo de espera.
4. Seleção do próximo processo.
5. Execução de uma unidade de CPU.
6. Verificação de:

   * Finalização;
   * Bloqueio por I/O;
   * Expiração do quantum.

Esse comportamento aproxima o simulador do funcionamento real de um sistema operacional.

---

# Métricas Coletadas

Ao final da execução são calculadas as seguintes métricas:

## Tempo Total

Quantidade total de unidades de tempo necessárias para finalizar todos os processos.

## CPU Ociosa

Quantidade de ciclos em que não havia nenhum processo disponível para execução.

## Utilização da CPU

Percentual efetivo de uso da CPU.

## Preempções

Número total de vezes em que um processo perdeu a CPU por consumir todo o quantum.

## Eventos de I/O

Quantidade de operações realizadas em:

* Disco
* Fita Magnética
* Impressora

## Tempo Médio de Espera

Média do tempo em que os processos permaneceram aguardando em filas de pronto.

## Turnaround Médio

Tempo médio entre a chegada e a finalização dos processos.

---

# Configuração da Seed

O código foi desenvolvido para permitir execução determinística ou aleatória.

Por padrão:

```java
private static final long SEED = 42L;
private static final boolean USAR_SEED_ALEATORIA = false;
```

Dessa forma, todas as execuções produzem os mesmos resultados, facilitando testes, demonstrações e correções.

Para gerar cenários diferentes, basta alterar:

```java
private static final boolean USAR_SEED_ALEATORIA = true;
```

---

# Execução com Docker

## Pré-requisitos

* Docker
* Docker Compose

---

## Estrutura Esperada

```text
.
├── Dockerfile
├── docker-compose.yml
├── README.md
└── src
    └── Round_robin.java
```

---

## Dockerfile

O projeto utiliza Java 17 através da imagem Eclipse Temurin.

Exemplo:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

CMD ["tail", "-f", "/dev/null"]
```

---

## docker-compose.yml

Exemplo:

```yaml
services:
  app:
    build: .
    container_name: java-app
    working_dir: /app
    volumes:
      - .:/app
    stdin_open: true
    tty: true
```

---

## Compilar e Executar

Construir a imagem:

```bash
docker compose up -d --build
```

Executar:

```bash
docker compose exec app mvn -q clean compile exec:java -Dexec.mainClass=com.trabalho.br.round_robin.Round_robin
```

Tambem é possivel só criar o container usando:
```bash
docker compose up -d
```

com isso é possivel entrar no conteiner:
```bash
docker compose exec app bash
```

Por uma questão de praticidade for criado o `executar.bat`
que basicamente constrói e roda o fonte java dentro do docker diretamente.

em um terminal:
```bash
executar.bat
```

# Exemplo de Saída

```text
[t=000] P1 chegou -> FILA ALTA
[t=000] CPU selecionou P1 (ALTA)

[t=001] Executando P1
[t=002] Executando P1
[t=003] Executando P1

[t=003] Quantum expirou -> P1 para FILA BAIXA

[t=004] P2 chegou -> FILA ALTA
[t=004] CPU selecionou P2 (ALTA)
```

Ao final:

```text
==================================================
RELATORIO FINAL
==================================================

Tempo total...............: 85
CPU ociosa................: 4
Utilizacao CPU............: 95.29%

Preempcoes................: 12

Eventos DISCO.............: 3
Eventos FITA..............: 2
Eventos IMPRESSORA........: 4

Tempo medio espera........: 8.70
Turnaround medio..........: 17.50
```

---

# Considerações Finais

O simulador desenvolvido demonstra os principais conceitos relacionados ao escalonamento de processos em sistemas operacionais, incluindo filas de prioridade, preempção, operações de entrada e saída e coleta de métricas de desempenho.

Além de atender aos requisitos do trabalho, a utilização de Docker garante que a aplicação possa ser executada de forma consistente em diferentes ambientes, simplificando a demonstração e a correção do projeto.
