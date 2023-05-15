import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Class SR
 * Define o objeto SR, ou seja, o objeto que guarda os valores necessários dos Servidores de Resolução.
 * Extende a classe Servidor.
 * <p>
 * 6 novembro 2022
 * Rita Vaz
 * Robert Szabo
 **/

public class SR extends sdns {
    // implementar cache

    // IP[:porta] do Servidor Primário
    private String SP;

    // IP[:porta] de um Servidor Secundário
    private String SS1;

    // IP[:porta] de um Servidor Secundário
    private String SS2;

    // O método inicializa a instância, sendo que é dada um valor de porta diferente da normalizada.
    public SR(Integer porta, Integer timeout, Boolean debug, String config, int type) throws IOException, ParseException
    {
        super(porta,timeout,debug,config,type);
    }

    // Método retorna o IP[:porta] de um SP.
    public String getSP()
    {
        return this.SP;
    }

    // Método retorna o IP[:porta] de um SS.
    public String getSS1()
    {
        return this.SS1;
    }

    // Método retorna o IP[:porta] de um SS.
    public String getSS2()
    {
        return this.SS2;
    }

    // Guarda, na instância, o IP[:porta] de um SP.
    public void setSP(String sp)
    {
        this.SP=sp;
    }

    // Guarda, na instância, o IP[:porta] de um SS.
    public void setSS1(String ss)
    {
        this.SS1=ss;
    }

    // Guarda, na instância, o IP[:porta] de um SS.
    public void setSS2(String ss)
    {
        this.SS2=ss;
    }

    // Inicializa a instância.
    public SR(SR sr) {
        super(sr);
    }

    // Método que devolve o clone da instância.
    public sdns clone(){
        return new SR(this);
    }

    /**
     * Método que guarda na cache as informações de configuração do servidor.
     * Recebe um ficheiro de cofiguração.
     * @throws ParseException
     * @throws IOException
     **/
    public void parserConfig(String filename) throws IOException, ParseException
    {
        List<String> lines = readFile(filename);

        // Lê linha a linha, a partir da lista linhas criada através do método readFile.
        for (String line : lines) {

            // Divide cada linha em partes através do método split, sendo que, usa como método de divisão o espaço.
            String[] part = line.split(" ");

            /**
             * Cada linha fica com as partes divididas da seguinte forma:
             *   parte[0] = Parâmetro
             *   parte[1] = Tipo de valor - tipos possíveis: SP, SS, DD, ST, LG
             *   parte[2] = Valor associado ao parametro
             **/


            /**
             * Apenas guarda informações se a linha não começar com '#', uma vez que este simbolo indica que tudo o que
             * aparece à frente são comentários.
             **/
            if (part[0] != "#")
            {
                switch(part[1])
                {
                    case "SP":
                        this.SP = part[2];
                        break;

                    case "SS":
                        if(this.SS1==null)
                            this.SS1 = part[2];
                        else this.SS2 = part[2];
                        break;

                    case "DD":
                        this.setDD(part[2]);
                        break;

                    case "ST":
                        if(part[0].compareTo("root")==0)
                            super.setFileServTopo(part[2]);
                        break;

                    case "LG":
                        if(part[0].compareTo("all")==0)
                            this.setFileLogAll(part[2]);
                        else this.setFileLog(part[2]);
                        break;
                }
            }
        }
        // foi detetada uma atividade interna no componente
        new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro de configuração lido", super.getFileLog(), super.getDebug());
    }
}
