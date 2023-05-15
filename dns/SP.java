import java.io.*;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Class SP
 * Define o objeto SP, ou seja, o objeto que guarda os valores necessários dos
 * Servidores Primários.
 * Extende a classe Servidor.
 * <p>
 * 6 novembro 2022
 * <p>
 * Rita Vaz
 * <p>
 * Robert Szabo
 **/
public class SP extends sdns {

    // As instâncias do objeto SP têm as seguintes variáveis:

    // O valor indica o caminho para o ficheiro da base de dados.
    private String fileDados;

    // IP[:porta] de um Servidor Secundário
    private String SS1;

    // IP[:porta] de um Servidor Secundário
    private String SS2;

    private int nLinhaDados;

    // O método inicializa a instância, sendo que é dada um valor de porta diferente
    // da normalizada.
    public SP(Integer porta, Integer timeout, Boolean debug, String config, int type) throws ClassNotFoundException, IOException, ParseException {
        super(porta, timeout, debug, config,type);
        this.nLinhaDados = super.data.parserDados(this.fileDados);
        
        // foi detetada uma atividade interna no componente
        new logs(sdf.format(new Date()), "EV","localhost", "Ficheiro de dados lido.", super.getFileLog(), debug);
        
        // Definir o socket do Servidor Primário (SP) com uma porta definida

        ServerSocket sp = new ServerSocket(6666);
        Thread workerTZ = new Thread(
                new tcpComm(nLinhaDados, super.getDomain(), super.data, super.getFileLog(), sp, super.getDebug(), this.getTimeout()));
        workerTZ.start();

    }

    // Inicializa a instância.
    public SP(SP sp) {
        super(sp);
        setFileDados(sp.getFileDados());
    }

    // Método retorna o caminho para o ficheiro de dados.
    public String getFileDados() {
        return this.fileDados;
    }

    // Método retorna o IP[:porta] de um SS.
    public String getSS1() {
        return this.SS1;
    }

    // Método retorna o IP[:porta] de um SS.
    public String getSS2() {
        return this.SS2;
    }

    // Guarda, na instância, o caminho para o ficheiro de dados.
    public void setFileDados(String fileDados) {
        this.fileDados = fileDados;
    }

    // Guarda, na instância, o IP[:porta] de um SS.
    public void setSS1(String ss) {
        this.SS1 = ss;
    }

    // Guarda, na instância, o IP[:porta] de um SS.
    public void setSS2(String ss) {
        this.SS2 = ss;
    }

    // Método que devolve o clone da instância.
    public sdns clone() {
        return new SP(this);
    }

    /**
     * Método que atribui às instâncias os respetivos valores.
     * Recebe um ficheiro de cofiguração.
     * @throws ParseException
     * @throws IOException
     **/
    public void parserConfig(String filename) throws IOException, ParseException {
        List<String> lines = readFile(filename);
        
        // Lê linha a linha, a partir da lista linhas criada através do método readFile.
        for (String line : lines) {

            // Divide cada linha em partes através do método split, sendo que, usa como
            // método de divisão o espaço.
            String[] part = line.split(" ");

            /**
             * Cada linha fica com as partes divididas da seguinte forma:
             * parte[0] = Parâmetro
             * parte[1] = Tipo de valor - tipos possíveis: DB, SS, DD, ST, LG
             * parte[2] = Valor associado ao parametro
             **/

            /**
             * Apenas guarda informações se a linha não começar com '#', uma vez que este
             * simbolo indica que tudo o que
             * aparece à frente são comentários.
             **/
            if (!(part[0].equals("#")) && !(part[0].equals(""))) {

                switch (part[1]) {

                    case "DB":
                        this.fileDados = part[2];
                        this.setDomain(part[0] + ".");
                        break;

                    case "SS":
                        if (this.SS1 == null)
                            this.SS1 = part[2];
                        else
                            this.SS2 = part[2];
                        break;

                    case "DD":
                        this.setDD(part[2]);
                        break;

                    case "ST":
                        if (part[0].compareTo("root") == 0)
                            this.setFileServTopo(part[2]);
                        break;

                    case "LG":
                        if (part[0].equals("all"))
                            this.setFileLogAll(part[2]);
                        else
                            this.setFileLog(part[2]);
                        break;
                }
            }
        }
        // foi detetada uma atividade interna no componente
        new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro de configuração lido", super.getFileLog(), super.getDebug());
    }
}
