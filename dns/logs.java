import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.locks.ReentrantLock;

public class logs {

    /**
     * Método que escreve, no final do ficheiro logs, o log criado seguindo a seguinte estrutura:
     * {etiqueta temporal} {tipo de entrada} {endereço IP[:porta]} {dados da entrada}
     * Recebe os valores como parâmetros
    **/

    ReentrantLock l = new ReentrantLock();

    public logs(String date, String type, String ip, String dados, String fileLog, Boolean debug) throws IOException, ParseException
    {
        if(fileLog.contains("all"))
        {
            l.lock();
            FileWriter fstream = new FileWriter(fileLog, true); //true tells to append data.
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(date + ", " + type + ", " + ip + ", " + dados + "\n");
            if(debug) System.out.print(date + ", " + type + ", " + ip + ", " + dados + "\n");
            out.close();
            l.unlock();
        } 
        else
        {
            FileWriter fstream = new FileWriter(fileLog, true); //true tells to append data.
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(date + ", " + type + ", " + ip + ", " + dados + "\n");
            if(debug) System.out.print(date + ", " + type + ", " + ip + ", " + dados + "\n");
            out.close();
        }
    }
}
