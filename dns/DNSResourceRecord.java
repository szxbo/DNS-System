/**
 * Class DNS Resource Record
 * Define o objeto DNSResourceRecord, ou seja, o objeto que guarda os parâmetros na cache.
 * <p> 9 novembro 2022
 * <p> Rita Vaz
 * <p> Robert Szabo
 **/

public class DNSResourceRecord {

    // Parametro.
    private String name;

    // Tipo de valor.
    private String type;

    // Valor.
    private String value;

    // Tempo de Validade.
    private Integer ttl;

    // Prioridade.
    private Integer order;

    // Define o tipo de Origens possíveis.
    private String origin;

    // Tempo, em segundos, que passou desde o arranque do servidor até ao momento em que a entrada foi registada na cache.
    private Integer timeStamp;

    // Número de entrada (as entradas válidas são numeradas/indexadas de 1 até N, em que N é o número total de entradas na
    // cache, incluindo as entradas livres/FREE).
    private Integer index;

    private String[] record;

    /**
     * Estado da entrada.
     * Considera-se que o estado da entrada apenas pode ser "FREE" ou "VALID"
     * Se o estado estiver FREE quer dizer que o tempo de validade da entrada já acabou e por isso pode ser substituida.
     * Se estiver VALID então a entrada está válida e pode ser utilizada.
     **/
    private String status;

    // Inicializa a instância.
    public DNSResourceRecord(String name, String type, String value, Integer ttl, Integer order, String origin, Integer index, Integer timeStamp, String status)
    {
        this.name=name;
        this.type=type;
        this.value=value;
        this.ttl=ttl;
        this.order=order;
        this.origin=origin;
        this.index=index;
        this.timeStamp=timeStamp;
        this.status=status;
    }

    public DNSResourceRecord(String rec)
    {
        record = rec.split(" ");
        this.name=record[0];
        this.type=record[1];
        this.value=record[2];
        this.ttl=Integer.parseInt(record[3]);
        this.order=Integer.parseInt(record[4]);
        this.origin=(record[5]);
        this.index=Integer.parseInt(record[6]);
        this.timeStamp=Integer.parseInt(record[7]);
        this.status=record[8];
    }

    public DNSResourceRecord(String rec, int index, int time)
    {
        record = rec.split(" ");
        this.name=record[0];
        this.type=record[1];
        this.value=record[2];
        this.ttl=Integer.parseInt(record[3]);
        this.order=0;
        this.origin="OTHERS";
        this.index=index;
        this.timeStamp=time;
        this.status="VALID";
    }


    // Método que retorna o nome do parâmetro.
    public String getName()
    {
        return this.name;
    }

    // Método que retorna o tipo do valor.
    public String getType()
    {
        return this.type;
    }

    // Método que retorna o valor, associado ao tipo de valor.
    public String getValue()
    {
        return this.value;
    }

    // Método que retorna o TTL, associado ao tipo de valor.
    public Integer getttl()
    {
        return this.ttl;
    }

    // Método que retorna a prioridade, associado ao tipo de valor.
    public Integer getOrder()
    {
        return this.order;
    }

    // Método que retorna a origem do valor.
    public String getOrigin()
    {
        return this.origin;
    }

    // Método que retorna o tempo em segundos que passou desde o arranque do servidor até ao
    // momento em que a entrada foi registada na cache.
    public Integer getTimeStamp()
    {
        return this.timeStamp;
    }

    // Método que retorna o número de entrada.
    public Integer getIndex()
    {
        return this.index;
    }

    // Método que retorna o número de estado da entrada.
    public String getStatus()
    {
        return this.status;
    }

    // Guarda o parâmetro.
    public void setName(String name)
    {
        this.name=name;
    }

    // Guarda o tipo de valor.
    public void setType(String type)
    {
        this.type=type;
    }

    // Guarda o valor.
    public void setValue(String value)
    {
        this.value=value;
    }

    // Guarda o TTL.
    public void setttl(Integer ttl)
    {
        this.ttl=ttl;
    }

    // Guarda a prioridade.
    public void setOrder(Integer order)
    {
        this.order=order;
    }

    // Guarda a origem da entrada.
    public void setOrigin(String origin)
    {
        this.origin=origin;
    }

    // Guarda o timeStamp.
    public void setTimeStamp(Integer timeStamp)
    {
        this.timeStamp=timeStamp;
    }

    // Guarda o número de entrada.
    public void setIndex(Integer index)
    {
        this.index=index;
    }

    // Guarda o status da entrada.
    public void setStatus(String status)
    {
        this.status=status;
    }

    public String toString(){
        return name + " " +
                type + " " +
                value + " " +
                ttl + " " +
                order + " " +
                origin + " " +
                index + " " +
                timeStamp + " " +
                status;
    }

    public String imprimeLinha() {
        if(order != 0) return name + " " +
                type + " " +
                value + " " +
                ttl + " " +
                order;
        else return name + " " +
                type + " " +
                value + " " +
                ttl;
    }

    public boolean equalsRec(DNSResourceRecord record)
    {
        return this.name.equals(record.name) && this.type.equals(record.type) && this.value.equals(record.value);
    }

}
