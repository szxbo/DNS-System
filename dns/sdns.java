import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.time.LocalDateTime;

/**
 * Define o objeto Servidor, ou seja, o objeto que guarda os valores dos
 * Servidores.
 * É uma classe abstrata.
 * <p>
 * 5 novembro 2022
 * <p>
 * Rita Vaz
 * <p>
 * Robert Szabo
 **/
public abstract class sdns {

    // As instâncias do objeto Servidor têm as seguintes variáveis:

    // O valor indica a porta de atendimento dos servidores.
    // A porta normalizada é a 53.
    private Integer porta;

    // O valor indica o número de vezes que os servidores enviam query à espera da
    // resposta.
    private Integer timeout;

    // O valor indica se o servidor funciona em modo debug.
    // O valor é True se funciona em modo debug.
    private Boolean debug;

    // O valor indica o caminho para ficheiro de configuração.
    private String fileConfig;

    // O valor indica o caminho para ficheiro com a lista de ST (servidor de topo).
    private String fileServTopo;

    // Lista de seervidores de topo
    private List<String> st;

    // O valor indica o caminho para ficheiro de logs. Estes ficheiros registam toda
    // a atividade relevante do componente.
    private String fileLog;

    // O valor indica o caminho para ficheiro de logs geral. Estes ficheiros
    // registam toda a atividade relevante de todos os componentes.
    private String fileLogAll;

    // O valor indica o domínio onde o servidor se encontra.
    private String domain;

    // O valor indica o servidor default.
    private String DD;

    // Momento em que ocorreu o arranque do servidor.
    public LocalDateTime start;

    // Socket UDP do servidor que terá a porta normalizada 53 e que receberá
    // queries.
    private DatagramSocket socket;

    // Formato da data guardado nos ficheiros logs
    // SimpleDateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");
    static DateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");

    // contém todas as informações guardadas na base de dados
    public Data data;

    // identifica o tipo de servidor
    // se type=0 -> SP
    // se type=1 -> SS
    // se type=2 -> SR
    public int type;

    /**
     * O método inicializa a instância, sendo que é dada um valor de porta diferente
     * da normalizada.
     * Considerou-se que a cache tem no máximo MAX linhas.
     * 
     * @throws IOException
     * @throws ParseException
     **/
    public sdns(Integer porta, Integer timeout, Boolean debug, String config, int type) throws IOException, ParseException {
        this.porta = porta;
        this.timeout = timeout;
        this.debug = debug;
        this.fileConfig = config;
        this.start = LocalDateTime.now();
        this.data = new Data(start);
        this.parserConfig(this.fileConfig);
        this.type=type;

        new logs(sdf.format(new Date()), "ST", "localhost", "porta: " + porta + " timeout: " + timeout + " debug: " + debug, fileLog, debug);
        this.st = new ArrayList<>();
        this.setServTopo();
        // foi detetada uma atividade interna no componente
        new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro ST lido", fileLog, debug);
    }

    // Inicializa a instância.
    public sdns(sdns s) {
        this.porta = s.getPorta();
        this.debug = s.getDebug();
        this.fileConfig = s.getFileConfig();
        this.fileServTopo = s.getFileServTopo();
        this.fileLog = s.getFileLog();
        this.start = LocalDateTime.now();
        this.data = s.getData();
        this.type=s.getType();
    }

    // Método devolve o valor da porta de atendimento.
    public Integer getPorta() {
        return this.porta;
    }

    // Método devolve o valor de timeout.
    public Integer getTimeout() {
        return this.timeout;
    }

    // Se o servidor funciona em modo debug, o método devolve true.
    public Boolean getDebug() {
        return this.debug;
    }

    // Método devolve o caminho para o ficheiro de configuração.
    public String getFileConfig() {
        return this.fileConfig;
    }

    // Método devolve o caminho para o ficheiro dos servidores de topo.
    public String getFileServTopo() {
        return this.fileServTopo;
    }

    // Método devolve o caminho para o ficheiro de logs.
    public String getFileLog() {
        return this.fileLog;
    }

    // Método devolve o caminho para o ficheiro de logs geral.
    public String getFileLogAll() {
        return this.fileLogAll;
    }

    // Método devolve o dominio onde o servidor se encontra.
    public String getDomain() {
        return this.domain;
    }

    // Método devolve o IP do servidor default.
    public String getDD() {
        return this.DD;
    }

    // Método devolve o objeto data.
    public Data getData() {
        return this.data;
    }

    // Método devolve o tipo de servidor.
    public int getType() {
        return this.type;
    }

    // Guarda o valor da porta de atendimento.
    public void setPorta(Integer porta) {
        this.porta = porta;
    }

    // Guarda o tempo de espera pela resposta.
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    // Guarda se o servidor funciona ou não em modo debug.
    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    // Guarda o caminho para o ficheiro de configuração.
    public void setFileConfig(String file) {
        this.fileConfig = file;
    }

    // Guarda o caminho para o ficheiro de servidores de topo.
    public void setFileServTopo(String servTopo) {
        this.fileServTopo = servTopo;
    }

    // Guarda a lista de servidores de topo.
    public void setServTopo() throws IOException, ParseException {
        List<String> lines;
        try {
            lines = this.readFile(this.fileServTopo);
            // Lê linha a linha, a partir da lista linhas criada através do método readFile.
            for (String line : lines)
            if (!line.trim().isEmpty() && line.charAt(0) != '#')
                this.st.add(line);
        } catch (FileNotFoundException e) {
            new logs(sdf.format(new Date()), "FL", "localhost" , "O ficheiro de ST não existe", this.fileLog, this.debug);
            e.printStackTrace();
        }
        
    }

    // Guarda o caminho para o ficheiro de logs.
    public void setFileLog(String log) {
        this.fileLog = log;
    }

    // Guarda o caminho para o ficheiro de logs geral.
    public void setFileLogAll(String log) {
        this.fileLogAll = log;
    }

    // Guarda o dominio onde se encontra o servidor.
    public void setDomain(String d) {
        this.domain = d;
    }

    // Guarda o IP do servidor default.
    public void setDD(String d) {
        this.DD = d;
    }

    // Guarda o IP do servidor default.
    public void setType(int t) {
        this.type = t;
    }

    private HashMap<String, String> alias = new HashMap<>();

    public void setAlias(HashMap<String, String> alias) {
        this.alias = alias;
    }

    public HashMap<String, String> getAlias() {
        return alias;
    }

    public abstract sdns clone();

    /**
     * Método que guarda na cache as informações de configuração do servidor.
     * Recebe um ficheiro de cofiguração.
     * @throws ParseException
     * @throws IOException
     **/
    public abstract void parserConfig(String filename) throws FileNotFoundException, IOException, ParseException;

    /**
     * Método que separa por linhas o ficheiro.
     * Recebe o caminho para um ficheiro.
     * Retorna uma lista de linhas.
     **/
    public List<String> readFile(String nameFile) throws FileNotFoundException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(nameFile), StandardCharsets.UTF_8);
        } catch (IOException exc) {
            lines = new ArrayList<>();
        }
        if (lines.isEmpty())
            throw new FileNotFoundException("Ficheiro não encontrado");
        return lines;
    }

    /**
     * Método que implementa a parte do servidor da comunicação UDP descrita no
     * Modelo de Comunicação.
     * Esta cria um socket com a porta 53 para receber queries, extrai a informação
     * necessária dos
     * datagramas enviados para saber para onde enviar a resposta. Também extrai os
     * dados da querie e
     * faz a pesquisa do nome e tipo pedidos na querie e constroi uma resposta para
     * enviar ao emissor da query.
     * No fim envia a resposta através de um novo socket com uma porta aleatória.
     * 
     * NÃO TERMINA!
     * @throws ParseException
     */
    public void runUDP() throws IOException, ParseException {
        socket = new DatagramSocket(porta);
        socket.setSoTimeout(this.timeout);
        boolean running = true;
        try {
            while (running) {
                // Buffer para armazennnar o datagrama que irá ser recebido
                byte[] buf = new byte[256];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                // Fica à espera até receber um datagrama
                socket.receive(packet);
                Thread worker = null;

                // Cria nova thread para responder à query
                worker = new Thread(
                    new udpComm(packet, this.fileLog, this.data, this.debug, this.st, this.DD));
                worker.start();
            }
        } catch (

        SocketTimeoutException e) {
            socket.close();
            new logs(sdf.format(new Date()), "TO", Integer.toString(this.porta) , "Timeout na espera da resposta a uma query", this.fileLog, this.debug);
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        sdns serv;

        /**
         * Métodos existentes de criação de instâncias de servidores:
         * public SP(Integer porta, Integer timeout, Boolean debug, String config)
         **/

        // Utiliza a porta normalizada e recebe menos um argumento.
        // args[2] = fileConfig

        try {
            int nArgs = args.length;

            if (nArgs == 1)
                if (args[0].equals("--help")) {
                    System.out.println("Argumentos válidos para inicialização do Servidor:" +
                            "\n-Argumentos opcionais-" +
                            "\n\t-p [porta]" +
                            "\n\t-t [timeout]" +
                            "\n\t-d [debug]" +
                            "\n-Argumento obrigatório- (inserir sempre no fim)\n\t[fileConfig]");
                    return;
                }

            // Valor da porta por predefinição
            int porta = 53;
            // Valor do timeout por prefinição
            int timeout = 60000000;
            // Modo debug ativo por predefinição
            boolean debug = true;
            // Ficheiro de configuração
            String fileConfig = "files/config/" + args[nArgs - 1];
            if (!fileConfig.contains(".conf"))
                fileConfig += ".conf";
            Scanner scanner = new Scanner(new FileInputStream(fileConfig));
            Boolean hasSP = false;
            Integer nSS = 0;
            String line;

            /**
             * Percorre o ficheiro de configuração à procura de definição de SP ou SS.
             * Se for um Servidor SP então não existe nenhuma entrada com o tipo SP.
             * Se for um Servidor SS então existe uma entrada SP e uma entrada SS.
             * Se for um Servidor SR então existe uma entrada SP e duas entradas SS.
             **/
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (line.contains("SP"))
                    hasSP = true;
                if (line.contains("SS"))
                    nSS += 1;
            }

            switch (nArgs) {
                case 1:
                    break;
                case 3:
                    switch (args[0]) {
                        case "-p":
                            porta = Integer.parseInt(args[1]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[1]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[1]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;

                    }
                    break;
                case 5:
                    switch (args[0]) {
                        case "-p":
                            porta = Integer.parseInt(args[1]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[1]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[1]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;
                    }
                    switch (args[2]) {
                        case "-p":
                            porta = Integer.parseInt(args[3]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[3]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[3]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;

                    }
                    break;
                case 7:
                    switch (args[0]) {
                        case "-p":
                            porta = Integer.parseInt(args[1]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[1]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[1]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;

                    }
                    switch (args[2]) {
                        case "-p":
                            porta = Integer.parseInt(args[3]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[3]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[3]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;

                    }
                    switch (args[4]) {
                        case "-p":
                            porta = Integer.parseInt(args[5]);
                            break;
                        case "-t":
                            timeout = Integer.parseInt(args[5]);
                            break;
                        case "-d":
                            debug = Boolean.parseBoolean(args[5]);
                            break;
                        default:
                            System.out.println("Insira argumentos válidos:" +
                                    "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                            return;
                    }
                    break;
                default:
                    System.out.println("Insira argumentos válidos:" +
                            "\n-p [porta]\n-t [timeout]\n-d [debug]\n[fileConfig]");
                    return;
            }

            // Inicializa o Servidor de acordo com o ficheiro de configuração e os
            // argumentos recebidos
            if (!hasSP)
                serv = new SP(porta, timeout, debug, fileConfig,0);
            else if (hasSP && nSS == 1)
                serv = new SS(porta, timeout, debug, fileConfig,1);
            else
                serv = new SR(porta, timeout, debug, fileConfig,2);

            

            // Verifica se o ficheiro de logs existe, e caso não exista, cria-o.
            File yourFile = new File(serv.getFileLog());
            yourFile.createNewFile();
            
            // foi detetada uma atividade interna no componente
            new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro de logs criado.", serv.getFileLog(), debug);
            
            serv.runUDP();

        } catch (FileNotFoundException e) {
            // Se o ficheiro não existir, o programa acaba.
            System.out.println("O ficheiro de Dados/Configuração do Servidor não existe.");
        } catch (NumberFormatException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}