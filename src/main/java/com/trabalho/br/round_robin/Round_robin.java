package com.trabalho.br.round_robin;

import java.util.*;

/**
 * Trabalho Final de Sistemas Operacionais Escalonador Round Robin com Feedback
 *
 * Implementação procedural conforme solicitado pelo enunciado: - Uma única
 * classe principal - Métodos estáticos - Estruturas simples - Records para PCB
 * e eventos de I/O
 *
 * Java 17
 */
public class Round_robin {

    // =====================================================
    // CONFIGURAÇÕES
    // =====================================================
    private static final int QUANTUM = 3;
    private static final int TOTAL_PROCESSOS = 10;

    /**
     * Para execução determinística.
     */
    private static final long SEED = 42L;

    /**
     * Defina como true para usar seed aleatória.
     */
    private static final boolean USAR_SEED_ALEATORIA = false;

    private static final int TEMPO_MIN_CPU = 5;
    private static final int TEMPO_MAX_CPU = 20;

    private static final int CHEGADA_MIN = 0;
    private static final int CHEGADA_MAX = 15;

    private static final int TEMPO_DISCO = 3;
    private static final int TEMPO_FITA = 5;
    private static final int TEMPO_IMPRESSORA = 8;

    // =====================================================
    // ENUMS
    // =====================================================
    enum Status {
        NOVO,
        PRONTO,
        EXECUTANDO,
        BLOQUEADO,
        FINALIZADO
    }

    enum Prioridade {
        ALTA,
        BAIXA
    }

    enum Dispositivo {
        NENHUM,
        DISCO,
        FITA,
        IMPRESSORA
    }

    // =====================================================
    // RECORDS
    // =====================================================
    record Processo(
            int pid,
            int ppid,
            int chegada,
            int tempoCpuTotal,
            int tempoCpuRestante,
            int instanteIO,
            int tempoIO,
            Dispositivo dispositivoIO,
            Prioridade prioridade,
            Status status,
            int tempoEspera,
            int tempoFinalizacao,
            boolean realizouIO,
            int cpuExecutada) {

    }

    record EventoIO(
            int pid,
            Dispositivo dispositivo,
            int retornoEm) {

    }

    // =====================================================
    // ESTRUTURAS GLOBAIS
    // =====================================================
    private static final List<Processo> processos = new ArrayList<>();

    private static final Queue<Integer> filaAlta = new LinkedList<>();
    private static final Queue<Integer> filaBaixa = new LinkedList<>();

    private static final List<EventoIO> filaIO = new ArrayList<>();

    private static int tempoAtual = 0;

    private static int cpuOciosa = 0;
    private static int preempcoes = 0;

    private static int eventosDisco = 0;
    private static int eventosFita = 0;
    private static int eventosImpressora = 0;

    private static Integer processoExecutando = null;
    private static int quantumRestante = 0;

    // =====================================================
    // MAIN
    // =====================================================
    public static void main(String[] args) {

        imprimirCabecalho();

        Random random = criarRandom();

        gerarProcessos(random);

        imprimirTabelaInicial();

        while (existemProcessosNaoFinalizados()) {

            processarChegadas();

            processarRetornosIO();

            atualizarTemposEspera();

            if (processoExecutando == null) {
                selecionarProcesso();
            }

            executarUmTick();

            tempoAtual++;
        }

        imprimirRelatorioFinal();
    }

    // =====================================================
    // RANDOM
    // =====================================================
    private static Random criarRandom() {
        if (USAR_SEED_ALEATORIA) {
            return new Random(gerarSeedAleatoria());
        }
        return new Random(SEED);
    }

    private static long gerarSeedAleatoria() {
        return System.currentTimeMillis();
    }

    // =====================================================
    // GERAÇÃO DE PROCESSOS
    // =====================================================
    private static void gerarProcessos(Random random) {

        for (int pid = 1; pid <= TOTAL_PROCESSOS; pid++) {

            int chegada
                    = CHEGADA_MIN
                    + random.nextInt(CHEGADA_MAX - CHEGADA_MIN + 1);

            int cpuTotal
                    = TEMPO_MIN_CPU
                    + random.nextInt(TEMPO_MAX_CPU - TEMPO_MIN_CPU + 1);

            Dispositivo dispositivo
                    = switch (random.nextInt(4)) {
                case 1 ->
                    Dispositivo.DISCO;
                case 2 ->
                    Dispositivo.FITA;
                case 3 ->
                    Dispositivo.IMPRESSORA;
                default ->
                    Dispositivo.NENHUM;
            };

            int instanteIO = -1;
            int tempoIO = 0;

            if (dispositivo != Dispositivo.NENHUM && cpuTotal >= 6) {
                instanteIO = 2 + random.nextInt(cpuTotal - 3);
                tempoIO
                        = switch (dispositivo) {
                    case DISCO ->
                        TEMPO_DISCO;
                    case FITA ->
                        TEMPO_FITA;
                    case IMPRESSORA ->
                        TEMPO_IMPRESSORA;
                    default ->
                        0;
                };
            }

            processos.add(
                    new Processo(
                            pid,
                            0,
                            chegada,
                            cpuTotal,
                            cpuTotal,
                            instanteIO,
                            tempoIO,
                            dispositivo,
                            Prioridade.ALTA,
                            Status.NOVO,
                            0,
                            -1,
                            false,
                            0
                    )
            );
        }
    }

    // =====================================================
    // CHEGADAS
    // =====================================================
    private static void processarChegadas() {

        for (int i = 0; i < processos.size(); i++) {
            Processo p = processos.get(i);
            if (p.status() == Status.NOVO
                    && p.chegada() <= tempoAtual) {

                atualizarProcesso(
                        p,
                        p.tempoCpuRestante(),
                        Prioridade.ALTA,
                        Status.PRONTO,
                        p.tempoEspera(),
                        p.tempoFinalizacao(),
                        p.realizouIO(),
                        p.cpuExecutada()
                );

                filaAlta.add(p.pid());
                log("P" + p.pid() + " chegou -> FILA ALTA");
            }
        }
    }

    // =====================================================
    // I/O
    // =====================================================
    private static void processarRetornosIO() {

        Iterator<EventoIO> iterator = filaIO.iterator();

        while (iterator.hasNext()) {
            EventoIO evento = iterator.next();

            if (evento.retornoEm() <= tempoAtual) {

                Processo p = localizar(evento.pid());

                Prioridade prioridadeRetorno
                        = switch (evento.dispositivo()) {

                    case DISCO ->
                        Prioridade.BAIXA;

                    case FITA,
                                 IMPRESSORA ->
                        Prioridade.ALTA;

                    default ->
                        Prioridade.ALTA;
                };

                atualizarProcesso(
                        p,
                        p.tempoCpuRestante(),
                        prioridadeRetorno,
                        Status.PRONTO,
                        p.tempoEspera(),
                        p.tempoFinalizacao(),
                        true,
                        p.cpuExecutada()
                );

                if (prioridadeRetorno == Prioridade.ALTA) {
                    filaAlta.add(p.pid());
                } else {
                    filaBaixa.add(p.pid());
                }

                log(
                        "P" + p.pid()
                        + " retornou de "
                        + evento.dispositivo()
                        + " -> FILA "
                        + prioridadeRetorno
                );

                iterator.remove();
            }
        }
    }

    // =====================================================
    // ESCALONADOR
    // =====================================================
    private static void selecionarProcesso() {

        if (!filaAlta.isEmpty()) {
            processoExecutando = filaAlta.poll();
            quantumRestante = QUANTUM;
            Processo p = localizar(processoExecutando);
            atualizarProcesso(
                    p,
                    p.tempoCpuRestante(),
                    p.prioridade(),
                    Status.EXECUTANDO,
                    p.tempoEspera(),
                    p.tempoFinalizacao(),
                    p.realizouIO(),
                    p.cpuExecutada()
            );
            log("CPU selecionou P" + processoExecutando + " (ALTA)");
            return;
        }

        if (!filaBaixa.isEmpty()) {
            processoExecutando = filaBaixa.poll();
            quantumRestante = QUANTUM;
            Processo p = localizar(processoExecutando);
            atualizarProcesso(
                    p,
                    p.tempoCpuRestante(),
                    p.prioridade(),
                    Status.EXECUTANDO,
                    p.tempoEspera(),
                    p.tempoFinalizacao(),
                    p.realizouIO(),
                    p.cpuExecutada()
            );
            log("CPU selecionou P" + processoExecutando + " (BAIXA)");
        }
    }

    // =====================================================
    // CPU
    // =====================================================
    private static void executarUmTick() {

        if (processoExecutando == null) {
            log("CPU OCIOSA");
            cpuOciosa++;
            return;
        }

        Processo p = localizar(processoExecutando);

        int restante = p.tempoCpuRestante() - 1;
        int executado = p.cpuExecutada() + 1;

        log("Executando P" + p.pid());
        atualizarProcesso(
                p,
                restante,
                p.prioridade(),
                Status.EXECUTANDO,
                p.tempoEspera(),
                p.tempoFinalizacao(),
                p.realizouIO(),
                executado
        );

        quantumRestante--;
        Processo atualizado = localizar(p.pid());
        if (!atualizado.realizouIO()
                && atualizado.dispositivoIO() != Dispositivo.NENHUM
                && atualizado.cpuExecutada() == atualizado.instanteIO()) {
            bloquearProcesso(atualizado);
            return;
        }

        if (atualizado.tempoCpuRestante() <= 0) {
            finalizarProcesso(atualizado);
            return;
        }

        if (quantumRestante <= 0) {
            preemptarProcesso(atualizado);
        }
    }

    private static void bloquearProcesso(Processo p) {

        atualizarProcesso(
                p,
                p.tempoCpuRestante(),
                p.prioridade(),
                Status.BLOQUEADO,
                p.tempoEspera(),
                p.tempoFinalizacao(),
                true,
                p.cpuExecutada()
        );

        filaIO.add(
                new EventoIO(
                        p.pid(),
                        p.dispositivoIO(),
                        tempoAtual + p.tempoIO()
                )
        );

        switch (p.dispositivoIO()) {
            case DISCO ->
                eventosDisco++;
            case FITA ->
                eventosFita++;
            case IMPRESSORA ->
                eventosImpressora++;
        }

        log(
                "P" + p.pid()
                + " BLOQUEADO para "
                + p.dispositivoIO()
                + " por "
                + p.tempoIO()
                + " unidades"
        );
        processoExecutando = null;
    }

    private static void finalizarProcesso(Processo p) {
        atualizarProcesso(
                p,
                0,
                p.prioridade(),
                Status.FINALIZADO,
                p.tempoEspera(),
                tempoAtual,
                p.realizouIO(),
                p.cpuExecutada()
        );
        log("P" + p.pid() + " FINALIZADO");
        processoExecutando = null;
    }

    private static void preemptarProcesso(Processo p) {
        atualizarProcesso(
                p,
                p.tempoCpuRestante(),
                Prioridade.BAIXA,
                Status.PRONTO,
                p.tempoEspera(),
                p.tempoFinalizacao(),
                p.realizouIO(),
                p.cpuExecutada()
        );
        filaBaixa.add(p.pid());
        preempcoes++;
        log("Quantum expirou -> P" + p.pid() + " para FILA BAIXA");
        processoExecutando = null;
    }

    // =====================================================
    // MÉTRICAS
    // =====================================================
    private static void atualizarTemposEspera() {
        for (int i = 0; i < processos.size(); i++) {
            Processo p = processos.get(i);
            if (p.status() == Status.PRONTO) {
                atualizarProcesso(
                        p,
                        p.tempoCpuRestante(),
                        p.prioridade(),
                        p.status(),
                        p.tempoEspera() + 1,
                        p.tempoFinalizacao(),
                        p.realizouIO(),
                        p.cpuExecutada()
                );
            }
        }
    }

    // =====================================================
    // RELATÓRIO FINAL
    // =====================================================
    private static void imprimirRelatorioFinal() {

        double turnaroundTotal = 0;
        double esperaTotal = 0;

        for (Processo p : processos) {
            int turnaround
                    = p.tempoFinalizacao() - p.chegada();

            turnaroundTotal += turnaround;
            esperaTotal += p.tempoEspera();
        }

        double turnaroundMedio
                = turnaroundTotal / processos.size();
        double esperaMedia
                = esperaTotal / processos.size();
        double utilizacaoCPU
                = ((double) (tempoAtual - cpuOciosa)
                / tempoAtual) * 100.0;

        System.out.println();
        System.out.println("==================================================");
        System.out.println("RELATORIO FINAL");
        System.out.println("==================================================");

        System.out.println("Tempo total...............: " + tempoAtual);
        System.out.println("CPU ociosa................: " + cpuOciosa);

        System.out.printf(
                Locale.US,
                "Utilizacao CPU...........: %.2f%%%n",
                utilizacaoCPU
        );

        System.out.println("Preempcoes................: " + preempcoes);

        System.out.println("Eventos DISCO............: " + eventosDisco);
        System.out.println("Eventos FITA.............: " + eventosFita);
        System.out.println("Eventos IMPRESSORA.......: " + eventosImpressora);

        System.out.printf(
                Locale.US,
                "Tempo medio espera.......: %.2f%n",
                esperaMedia
        );

        System.out.printf(
                Locale.US,
                "Turnaround medio.........: %.2f%n",
                turnaroundMedio
        );

        System.out.println();
        System.out.println("DETALHAMENTO DOS PROCESSOS");

        for (Processo p : processos) {

            int turnaround
                    = p.tempoFinalizacao() - p.chegada();

            System.out.printf(
                    Locale.US,
                    "P%-2d | chegada=%2d | cpu=%2d | espera=%2d | turnaround=%2d%n",
                    p.pid(),
                    p.chegada(),
                    p.tempoCpuTotal(),
                    p.tempoEspera(),
                    turnaround
            );
        }
    }

    // =====================================================
    // UTILITÁRIOS
    // =====================================================
    private static Processo localizar(int pid) {
        for (Processo p : processos) {
            if (p.pid() == pid) {
                return p;
            }
        }
        throw new IllegalStateException("PID não encontrado: " + pid);
    }

    private static void atualizarProcesso(
            Processo original,
            int cpuRestante,
            Prioridade prioridade,
            Status status,
            int tempoEspera,
            int tempoFinalizacao,
            boolean realizouIO,
            int cpuExecutada
    ) {
        int indice = processos.indexOf(original);
        processos.set(
                indice,
                new Processo(
                        original.pid(),
                        original.ppid(),
                        original.chegada(),
                        original.tempoCpuTotal(),
                        cpuRestante,
                        original.instanteIO(),
                        original.tempoIO(),
                        original.dispositivoIO(),
                        prioridade,
                        status,
                        tempoEspera,
                        tempoFinalizacao,
                        realizouIO,
                        cpuExecutada
                )
        );
    }

    private static boolean existemProcessosNaoFinalizados() {
        for (Processo p : processos) {
            if (p.status() != Status.FINALIZADO) {
                return true;
            }
        }
        return false;
    }

    private static void log(String mensagem) {
        System.out.printf(
                "[t=%03d] %s%n",
                tempoAtual,
                mensagem
        );
    }

    private static void imprimirCabecalho() {
        System.out.println("==================================================");
        System.out.println("SIMULADOR ROUND ROBIN COM FEEDBACK");
        System.out.println("==================================================");
        System.out.println("Quantum : " + QUANTUM);
        System.out.println("Processos: " + TOTAL_PROCESSOS);
        System.out.println("Seed     : "
                + (USAR_SEED_ALEATORIA ? "ALEATORIA" : SEED));
        System.out.println("==================================================");
        System.out.println();
    }

    private static void imprimirTabelaInicial() {
        System.out.println("PROCESSOS GERADOS");
        System.out.println();
        for (Processo p : processos) {
            System.out.printf(
                    Locale.US,
                    "P%-2d chegada=%2d cpu=%2d io=%s instanteIO=%2d%n",
                    p.pid(),
                    p.chegada(),
                    p.tempoCpuTotal(),
                    p.dispositivoIO(),
                    p.instanteIO()
            );
        }
        System.out.println();
    }
}
