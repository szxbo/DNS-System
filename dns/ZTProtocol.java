import java.io.Serializable;

/**
 * Protocolo aplicacional que funcionará sobre TCP para efetuar transferência
 * de dados da base de dados de um Servidor Primário para um Servidor Secundário.
 * Também servirá de controlo para a Transferência de Zona (TZ), nomeadamente para enviar
 * o nome do domínio para o qual se deseja efetuar a TZ, número de linhas a enviar e eventuais erros.
 * <p> 17 novembro 2022
 * <p> Rita Vaz
 * <p> Robert Szabo
 */
public class ZTProtocol implements Serializable {

    /**
     *  Variável que irá definir o tipo da mensagem enviada.
     *  <p></p>Type == 0: Nome do domínio;
     *  <p></p>Type == 1: Número de linhas a enviar;
     *  <p></p>Type == 2: Envio da linha da base de dados;
     *  <p></p>Type == -1: Erro.
     */
    private int type;

    // Variável que identifica o número da mensagem enviada.
    private int seqNumb;

    // Dados relativos ao tipo da mensagem a ser enviada.
    private String value;

    // Construtor do objeto do tipo ZTProtocol
    public ZTProtocol(int type, int seqNumb, String value){
        this.type = type;
        this.seqNumb = seqNumb;
        this.value = value;
    }
    // Método que devolve o tipo da mensagem.
    public int getType() {
        return type;
    }
    // Método devolve a identificação numérica da mensagem.
    public int getSeqNumb() {
        return seqNumb;
    }
    // Método que devolve os dados relativos ao tipo da mensagem.
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ZTProtocol{" +
                "type=" + type +
                ", seqNumb=" + seqNumb +
                ", value='" + value +
                '}';
    }
}