import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class udpComm implements Runnable {

    // Formato da data guardado nos ficheiros logs
    private DateFormat sdf = new SimpleDateFormat("dd:mm:yyyy.HH:mm:ss:SSS");

    private DatagramPacket packet;

    private String fileLog;

    private Data data;

    private Boolean debug;

    private List<String> st;

    private String DD;

    public udpComm(DatagramPacket packet, String fileLog, Data data, Boolean debug, List<String> st, String DD) {
        this.packet = packet;
        this.fileLog = fileLog;
        this.data = data;
        this.debug = debug;
        this.st = st;
        this.DD = DD;
    }

    public DNSHandler test(DatagramSocket socket2, String ip, String porta) throws ParseException, IOException
    {
        // cria datagrama para enviar para o servidor
        InetAddress ipAddress = InetAddress.getByName(ip);
        DatagramPacket queryRep = new DatagramPacket(packet.getData(), packet.getData().length,
                ipAddress,
                Integer.parseInt(porta));

        DNSHandler response = new DNSHandler(packet.getData());
        
        // envia packet para o servidor
        socket2.send(queryRep);

        // foi enviada uma query para o endereço indicado
        new logs(sdf.format(new Date()), "QE", ipAddress.toString() + ":" + porta,
                response.printPDUConciso(), fileLog,
                debug);

        // Buffer para receção da resposta.
        byte[] buf = new byte[1024];

        // Criação de um datagrama que irá armazenar a resposta recebida pelo socket
        // para poder ser descodificada.
        DatagramPacket packet2 = new DatagramPacket(buf, buf.length);

        // fica à espera da respota
        try{ socket2.receive(packet2);} 
        catch (SocketTimeoutException e) {
            // se o servidor não responde, tentar com outro
            new logs(sdf.format(new Date()), "TO", ipAddress.toString() + ":" + porta,
                        "Resposta a uma query", fileLog, false);
            return null;
            
        }
        
        // Extrai payload do datagrama
        response = new DNSHandler(packet2.getData());

        return response;
    }

    public DNSHandler aux(DatagramSocket socket2) throws ParseException, IOException
    {
        // o DD não tinha a resposta ou não respondeu
                
        boolean done = false;
        String ip = "", ipAnterior = "", porta;

        // começa por procurar no ST
        int iST = 0;

        // pega no ip e porta do ST
        String ipPort = st.get(iST);
        String[] part = ipPort.split(":");
        ip = part[0];
        porta = part[1];

        DNSHandler response = test(socket2, ip, porta);

        // enquanto os ST não respondem
        while(response==null && iST<st.size()-1)
        {
            iST++;
            ipPort = st.get(iST);
            String[] part2 = ipPort.split(":");
            ip = part2[0];
            porta = part2[1];
            response = test(socket2, ip, porta);
        }

        // não encontrou nenhum ST
        if(iST==st.size())
            return null;
        
        DNSHandler responseST = response.clone();
                    
        // enquanto não tiver respostas e o proximo ip a procurar não for igual ao ip anterior
        while (response.getnValues() == 0 && !done) 
        {
            int count = 0;
            // pega ip do proximo servidor a perguntar
            String line = response.getExtra()[count];
            String[] part2 = line.split(" ");
            ip = part2[2];
            porta = "53";

            if (ipAnterior.equals(ip)) 
                done = true;
            
            
            if (!done) 
            {
                response = test(socket2, ip, porta);

                // foi recebida uma resposta do endereço indicado
                // new logs(sdf.format(new Date()), "RR", ipAddress.toString() + ":" + porta,
                // response.printPDUConciso(), fileLog,
                // debug);

                ipAnterior = ip;
                
                if (response==null) 
                {
                    // SP não responde
                    

                    int i = 0;

                    // pesquisar nos ss -> o ip está nas extra values da response
                    count ++;

                    // pega no ip e porta do SS
                    line = responseST.getExtra()[count];
                    part2 = line.split(" ");
                    ip = part2[2];
                    porta = "53";
                    response = test(socket2, ip, porta);
                    
                    // enquanto os SS não respondem
                    while(response==null && count<responseST.getnExtraValues()-1)
                    {
                        count++;
                        line = responseST.getExtra()[count];
                        part2 = line.split(" ");
                        ip = part2[2];
                        response = test(socket2,ip,porta);
                    }
                    
                    // não encontrou nenhum ss a funcionar
                    if(i==response.getnExtraValues())
                        return null;
                }
                
            }
        }

        return response;
    }

    public void run() {
        try {
            // Extrai payload do datagrama para pesquisa na base de dados/cache.
            DNSHandler query = new DNSHandler(packet.getData());

            // Extrai o IP e Port do cliente, respetivamente para preparar o pacote de
            // resposta.
            InetAddress address = packet.getAddress();
            int port = packet.getPort();

            // foi recebida uma query do endereço indicado
            new logs(sdf.format(new Date()), "QR", address.toString() + ":" + port, query.printPDUConciso(), fileLog,
                    debug);

            DNSHandler response = null;

            // Cria um socket com uma porta aleatória para enviar a resposta.
            int randomPort = (int) ((Math.random() * (65353 - 1024)) + 1024);
            // abre socket
            DatagramSocket socket2 = new DatagramSocket(randomPort);

            if (this.fileLog.contains("sr"))
            {
                // Pesquisa na cache
                // Constroi a resposta para a query recebida anteriormente.
                response = data.buildResponse2(query);

                if (response.getnValues() == 0) 
                {
                    // && response.getnAuthorities() == 0 && response.getnExtraValues() == 0

                    // colocaelse timeout no socket -> tempo de espera por respostas
                    socket2.setSoTimeout(2500);

                    String ip = "", porta = "";

                    // pega no ip e porta do DD
                    if(this.DD.contains(":"))
                    {
                        // é indicada a porta
                        String[] part = this.DD.split(":");
                        ip = part[0];
                        porta = part[1];
                    }
                    else
                    {
                        // usa porta por defeito
                        ip = this.DD;
                        porta = "53";
                    }

                    response = this.test(socket2,ip,porta);

                    if(response!=null && response.getnValues()==0 && (response.getnAuthorities()!=0 || response.getnExtraValues()!=0)
                    && response.getResponseCode()!=2)
                    {
                        // o dd conhece o próximo servidor

                        // pega ip do proximo servidor a perguntar
                        String line = response.getExtra()[0];
                        String[] part2 = line.split(" ");
                        ip = part2[2];
                        porta = "53";

                        response = this.test(socket2,ip,porta);

                    }


                    // se não existir a resposta no DD, procurar no ST
                    else if(response==null || response.getnValues() == 0)
                        response = this.aux(socket2);


                    // Adiciona a resposta à cache
                    data.addResponseToCache(response);

                }
                else
                {
                    // a resposta veio da cache do SR, retirar flag A
                    response.setFlags("");
                }
            }
            else{
                // Pesquisa na cache
                // Constroi a resposta para a query recebida anteriormente.
                response = data.buildResponse(query);
            }

            // cria pacote de resposta
            packet = new DatagramPacket(response.toString().getBytes(), response.toString().getBytes().length,
                    address, port);

            socket2.send(packet);

            // foi enviada uma resposta do endereço indicado
            new logs(sdf.format(new Date()), "RP", address.toString() + ":" + port, response.printPDUConciso(),
                    fileLog,
                    debug);
                
            
            // fecha socket
            socket2.close();

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
