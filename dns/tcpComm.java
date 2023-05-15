import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class tcpComm implements Runnable {

    private int nLinhaDados;
    private String domain;
    private String fileLog;
    private ServerSocket sp;
    private Boolean debug;
    private String err;
    private int timeout;

    // Formato da data guardado nos ficheiros logs
    private DateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");

    private Data data;

    public tcpComm(int nLinhaDados, String domain, Data data, String fileLog, ServerSocket sp, Boolean b, int timeout) {
        this.nLinhaDados = nLinhaDados;
        this.domain = domain;
        this.data = data;
        this.fileLog = fileLog;
        this.sp = sp;
        this.debug = b;
        this.timeout=timeout;
    }

    private HashMap<String, String> alias = new HashMap<>();

    public void setAlias(HashMap<String, String> alias) {
        this.alias = alias;
    }

    public HashMap<String, String> getAlias() {
        return alias;
    }

    /**
     * Método que analisa mensagem vinda do Servidor Secundário e produz uma
     * resposta
     * conforme o tipo da mensagem e os valores indicados.
     * Serve para o SP
     */
    private ZTProtocol buildResponseZT(ZTProtocol mensagem, DNSResourceRecord[] dados, int pos) {
        int type = mensagem.getType();
        int seqNumb = mensagem.getSeqNumb();
        String value = mensagem.getValue();
        ZTProtocol resposta = null;

        /*
         * Indica que a mensagem contém o nome do domínio que será
         * analisado pelo SP para confirmar se é o seu domínio.
         */
        if (type == 0) {
            if (value.equals(this.domain)) {
                // Constrói a resposta referente ao número de linhas.
                resposta = new ZTProtocol(1, ++seqNumb, String.valueOf(dados.length));
            } else
                resposta = new ZTProtocol(-1, ++seqNumb, null);
            // Transferência da linha da base de dados
        } else {
            resposta = new ZTProtocol(2, ++seqNumb, dados[pos].toString());
        }
        return resposta;
    }

    /**
     * Método que estabelece uma conexão TCP no Servidor Primário à espera de
     * receber um pedido de Transferência de Zona
     * de um Servidor Secundário e realiza a transferência da base de dados.
     */
    public void run() {
        try {
            // Array auxiliar que contém os dadas da base de dados
            DNSResourceRecord[] dados = new DNSResourceRecord[nLinhaDados];
            // Percorre a cache e copia linhas referentes a dados da base de dados, para um
            // array auxiliar.
            for (int i = 0, j = 0; i < nLinhaDados; i++, j++)
                if (this.data.cache[i].getOrigin().equals("FILE")) {
                    dados[j] = this.data.cache[i];
                }
            // Variavel de controlo do ciclo que realiza a transferência das linhas da base
            // de dados.
            boolean running = true;

            while (running) {
                // Esperar por conexões de Servidores Secundários (SS)
                Socket ss = sp.accept();

                // Timeout caso não receba nenhuma resposta em timeout segundos
                ss.setSoTimeout(this.timeout);

                // Guarda o endereço para o caso de haver erros na transferência de zona
                this.err = ss.getInetAddress().getHostAddress();
                
                // Imprime o IP do SS conectado
                // Definição da stream de entrada de dados no SP
                ObjectInputStream in = new ObjectInputStream(ss.getInputStream());
                // Definição da stream de saída de dados do SP
                ObjectOutputStream out = new ObjectOutputStream(ss.getOutputStream());

                /*
                 * ------------------------------------------------------------------------
                 * INÍCIO DA TRANSFERÊNCIA DE ZONA (TZ)
                 * ------------------------------------------------------------------------
                 */

                /*
                 * Leitura e armazenamento da mensagem a partir da stream de entrada,
                 * contendo o domínio do qual o SS quer realizar a cópia de base de dados.
                 */
                ZTProtocol mensagem = (ZTProtocol) in.readObject();
                // Construção e envio da resposta em relação à permissão para a TZ
                ZTProtocol resposta = buildResponseZT(mensagem, dados, -1);
                out.writeObject(resposta);
                // Mensagem de confirmação para permissão do envio
                mensagem = (ZTProtocol) in.readObject();

                // Inicio do processo de transferência das linhas da base de dados
                if (mensagem.getType() == 1 && mensagem.getValue().equals(resposta.getValue())) {
                    // Variável que contém o número de linhas da base de dados enviadas.
                    int i = 0;
                    ZTProtocol linha, confLinha = mensagem;
                    while (i < nLinhaDados) {
                        linha = buildResponseZT(confLinha, dados, i);
                        out.writeObject(linha);
                        confLinha = (ZTProtocol) in.readObject();
                        if (confLinha.getType() == 2 && confLinha.getSeqNumb() == linha.getSeqNumb())
                            i++;
                    }
                    // Fechar streams de entrada e saída de dados
                    in.close();
                    out.close();

                    // Foi iniciado e concluído corretamente um processo de transferência de zona
                    // O endereço indica o servidor na outra ponta da transferência;
                    // Os dados da entrada indicam que é um SP.

                    new logs(sdf.format(new Date()), "ZT", ss.getInetAddress().getHostAddress(), "SP", fileLog, debug);

                    // Fechar sockets de comunicação e conexão
                    ss.close();
                }
            }

        } catch (ParseException | ClassNotFoundException e) {
            // foi detetado um erro na transferência de zona
            try {
                // o endereço deve indicar o servidor na outra ponta da transferência; os dados da
                // entrada devem indicar qual o papel do servidor local na transferência (SP ou SS);
                new logs(sdf.format(new Date()), "EZ", this.err, "SP", this.fileLog, debug);
            } catch (IOException | ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        catch (SocketException e)
        {
            // timeout
            try {
                new logs(sdf.format(new Date()), "TO", this.err, "Iniciar transferência de Zona", this.fileLog, debug);
            } catch (IOException | ParseException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
