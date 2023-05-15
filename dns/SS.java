import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Objects;
import java.text.ParseException;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Class SS
 * Define o objeto SS, ou seja, o objeto que guarda os valores necessários dos Servidores Secundários.
 * Extende a classe Servidor.
 * <p> 6 novembro 2022
 * <p> Rita Vaz
 * <p> Robert Szabo
 **/

public class SS extends sdns {

    // IP[:porta] do Servidor Primário
    private String SP;

    // IP[:porta] do Servidor Secundário
    private String SS;

    // O método inicializa a instância, sendo que é dada um valor de porta diferente da normalizada.
    public SS(Integer porta, Integer timeout, Boolean debug, String config, int type) throws ClassNotFoundException, IOException, ParseException
    {
        super(porta,timeout,debug,config,type);
        runTCP();
    }

    // Inicializa a instância.
    public SS(SS ss){
        super(ss);
    }

    // Método retorna o IP[:porta] do SP.
    public String getSP()
    {
        return this.SP;
    }

    // Método retorna o IP[:porta] do SS.
    public String getSS()
    {
        return this.SS;
    }

    // Guarda, na instância, o IP[:porta] do SP.
    public void setSP(String sp)
    {
        this.SP=sp;
    }

    // Guarda, na instância, o IP[:porta] de um SS.
    public void setSS(String ss)
    {
        this.SS=ss;
    }

    // Método que devolve o clone da instância.
    public sdns clone(){
        return new SS(this);
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

            /*
             * Cada linha fica com as partes divididas da seguinte forma:
             *   parte[0] = Parâmetro
             *   parte[1] = Tipo de valor -  tipos possíveis: SP, SS, DD, ST, LG
             *   parte[2] = Valor associado ao parametro
             */

            /*
             * Apenas guarda informação se a linha não começar com '#', visto que este simbolo indica que tudo o que
             * aparece à frente são comentários.
             */
            if (!Objects.equals(part[0], "#"))
            {
                switch(part[1])
                {
                    case "SP":
                        this.SP = part[2];
                        this.setDomain(part[0]+".");
                        break;

                    case "SS":
                        this.SS = part[2];
                        break;

                    case "DD":
                        this.setDD(part[2]);
                        break;

                    case "ST":
                        if(part[0].compareTo("root")==0)
                            this.setFileServTopo(part[2]);
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

    private void runTCP() throws IOException, ClassNotFoundException, ParseException {
        
        // Iniciar conexão com o Servidor Primário (SP).
        InetAddress spIP = InetAddress.getByName(SP);
        Socket s = new Socket(spIP, 6666);
        // Definir stream de saída de dados do Servidor Secundário (SS).
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        //CountingInputStream countingStream = new CountingInputStream(s.getInputStream());
        // Definir stream de entrada de dados no SS
         ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        //ObjectInputStream in = new ObjectInputStream(countingStream);

        /* ------------------------------------------------------------------------
                             INÍCIO DA TRANSFERÊNCIA DE ZONA (TZ)
           ------------------------------------------------------------------------*/

        // Criação da primeira mensagem que envia o domínio para do qual quer receber a cópia da base de dados.
        ZTProtocol mensagem = new ZTProtocol(0,1,getDomain());
        // Envia mensagem com o domínio pela stream de saída
        out.writeObject(mensagem);
        // Resposta de confirmação ao pedido de transferência, contendo já o número de linhas da base de dados.
        ZTProtocol resposta = (ZTProtocol) in.readObject();
        int nLinhas = 0;
        if(resposta.getType() == -1)
        {
            new logs(sdf.format(new Date()), "EZ", this.SP, "SS", super.getFileLog(), super.getDebug());    
        }
        else nLinhas = Integer.parseInt(resposta.getValue());
        // Mensagem com a confirmação do número de linha que dará início à TZ
        mensagem = buildResponseZT(resposta,-1);
        out.writeObject(mensagem);

        LocalDateTime before = LocalDateTime.now();
        

        // Início do processo de transferência das linhas da base de dados
        // Variável que contém o número de linhas da base de dados recebidas
        int i = 0;
        ZTProtocol linha, confLinha;
        while(i < nLinhas) {
            linha = (ZTProtocol) in.readObject();
            String[] part = linha.getValue().split(" ");
            confLinha = buildResponseZT(linha,i);
            out.writeObject(confLinha);
            // Insere a linha transferida na cache do SS
            if (super.data.cache[i].getStatus().equals("FREE")) {
                super.data.cache[i] = new DNSResourceRecord(linha.getValue());
            }
            // Cria o HashMap que contém os sinónimos de CNAME
            if(part[1].equals("CNAME")) super.getAlias().put(part[0],part[2]);
            i++;
        }
       
        LocalDateTime after = LocalDateTime.now();

        long diff = ChronoUnit.MILLIS.between(before, after);

        // Fechar streams de entrada e saída de dados
        in.close();
        out.close();
        // Fechar o socket
        s.close();


        // Foi iniciado e concluído corretamente um processo de transferência de zona
        // O endereço indica o servidor na outra ponta da transferência; 
        // Os dados da entrada indicam que é um SS e o tempo da transferência de zona, em milissegundos.
        try { new logs(sdf.format(new Date()), "ZT",SP, "SS: transfer time: " + diff + " milissegundos", super.getFileLog(), super.getDebug());}
        catch( ParseException e )
        { 
            // Houve erro na transferência de Zona
            new logs(sdf.format(new Date()), "EZ", this.SP, "SS", super.getFileLog(), super.getDebug());
        }
    }
    /**
     * Método que analisa mensagem vinda do Servidor Primário e produz uma resposta
     * conforme o tipo da mensagem e os valores indicados.
     */
    private ZTProtocol buildResponseZT(ZTProtocol mensagem, int pos) {
        int type = mensagem.getType();
        int seqNumb = mensagem.getSeqNumb();
        String value = mensagem.getValue();
        ZTProtocol resposta = null;

        // Constrói resposta com a confirmação do número de linhas.
        if (type == 1) {
            resposta = new ZTProtocol(type,++seqNumb,value);
        // Contrói resposta com a confirmação da receção da linha transferida.
        } else {
            resposta = new ZTProtocol(type, seqNumb, value);
        }
        return resposta;
    }

}

