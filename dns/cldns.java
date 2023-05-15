import java.lang.management.ManagementFactory;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;

/**
 * Classe que implementa a funcionalidade de um cliente comunicar com um
 * Servidor
 * por UDP, podendo enviar queries (perguntas) com um Nome e um Tipo sobre um
 * determinado domínio.
 * Excecionalmente (e temporareamente) esta classe implementa a funcionalidade
 * de terminar as operações de receção
 * de queries de um determinado Servidor.
 * <p>
 * 11 novembro 2022
 * <p>
 * Robert Szabo
 */
public class cldns {
    private DatagramSocket socket;
    private InetAddress address;

    // Variável que contém um número de porta aleatório não podendo ter valores de
    // Well Known Ports.
    int port = (int) ((Math.random() * (65353 - 1024)) + 1024);

    // Formato da data guardado nos ficheiros logs
    private DateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");

    /**
     * Método que implementa a comunicação UDP com um Servidor, enviando e recebendo
     * mensagens do tipo
     * DNSHandler que representam as queries e respostas sobre domínios
     * Recebe como argumento o ip e porta do SR do domínio
     **/
    public void runUDP(String ip, String porta, String name, String value, Boolean debug) throws IOException, ParseException {
        
        // Criação de um socket no IP e Porta recebidos por argumentos no terminal
        address = InetAddress.getByName(ip);
        socket = new DatagramSocket();
        // Timeout caso não receba nenhuma resposta em 5 segundos
        socket.setSoTimeout(10000);
        // Criação da query para ser enviada para o Servidor
        DNSHandler query = new DNSHandler((int) ManagementFactory.getRuntimeMXBean().getPid(), name, value);
        new DNSHandler((int) ManagementFactory.getRuntimeMXBean().getPid(), name, value);
        String logsFile = "files/logs/cl.log";

        // Buffer que irá armazenar a query para poder ser convertida para bytes e
        // enviada pelo socket.
        byte[] buf = query.toString().getBytes();
        // Criação de um datagrama UDP que contém os dados de uma query em bytes, o IP e
        // Porta do Servidor.
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, Integer.parseInt(porta));
        // Envio do datagrama pelo socket.
        socket.send(packet);

        if(debug)
            new logs(sdf.format(new Date()), "QE", address.toString() + ":" + porta,
                            name + " " + value, logsFile, false);

        // Buffer para receção da resposta enviada pelo Servidor pelo socket.
        buf = new byte[1024];
        // Criação de um datagrama que irá armazenar a resposta recebida pelo socket
        // para poder ser descodificada.
        DatagramPacket packet2 = new DatagramPacket(buf, buf.length);
        try {
            // Recebimento dos dados pelo socket e armazenamento da informação no datagrama
            // criado
            socket.receive(packet2);
            // Extração do payload (em bytes) que contém a informação sobre a resposta e
            // reconstrução da mensagem para poder ser interpretada.
            DNSHandler response = new DNSHandler(packet2.getData());

            if(debug) 
                new logs(sdf.format(new Date()), "RR", address.toString() + ":" + porta,
                        response.printPDUConciso(), logsFile, false);
            
            response.printPDU();
        } catch (SocketTimeoutException e) {
            //System.out.println("Timeout da conexão.");
            new logs(sdf.format(new Date()), "TO", address.toString() + ":" + porta,
                        "Resposta a uma query", logsFile, false);
        }
        socket.close();
    }

    public static void main(String[] args) throws IOException {
        cldns cliente = new cldns();
        DateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");
        try{
        if (args.length == 3)
            cliente.runUDP(args[0], "53", args[1], args[2],false);
        else if (args.length == 4)
        {
            if(args[3].equals("-d")) 
            {
                cliente.runUDP(args[0], "53", args[1], args[2], true);
                // foi detetada uma atividade interna no componente
                new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro de logs criado.", "files/logs/cl.log", true);
            }
            else cliente.runUDP(args[0], args[1], args[2], args[3], false);
        }
        else if(args.length == 5)
        {
            cliente.runUDP(args[0], args[1], args[2], args[3], true);
            // foi detetada uma atividade interna no componente
            new logs(sdf.format(new Date()), "EV", "localhost", "Ficheiro de logs criado.", "files/logs/cl.log", true);
        }
        else
            System.out.println("Insira argumentos válidos:\n" + 
                                "[IP] [QueryName] [QueryType]\n" + "\t\tou\n" +
                                "[IP] [Porta] [QueryName] [QueryType]\n" + 
                                "Opcional: Acrescentar flag '-d' no final para ativar modo Debug");
        }
        catch (ParseException e)
        {
            System.out.println("Erro ao abrir o ficheiro de logs");
        }
    }
}
