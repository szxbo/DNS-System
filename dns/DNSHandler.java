/**
 * A classe DNSHandler implementa e lida com o protocolo aplicacional do
 * definido no Modelo de Comunicação.
 * Este protocolo aplicacional funcionará sobre o protocolo de transporte UDP.
 * A estrutura definida contém um cabeçalho com um número de mensagem que será o
 * identificador de cada mensagem.
 * <p>
 * Contém as flags que podem ser (Q),(A),(R). A flag Q ativa indica que a
 * mensagem é uma query, quando estiver inativa
 * significa que é uma resposta a uma mensagem enviada anteriormente. A flag (A)
 * ativa perimite identificar se o emissor da
 * mensagem é autoritativo sobre o domínio ao qual responde. A flag (R) ativa é
 * utilizada para pedir que a query seja
 * respondida de forma recursiva, caso contrário, o metódo será iterativo.
 * <p>
 * Os campos nValues, nAuthorities, nExtraValues indicam quantas entradas
 * existem nos campos values, authorities, extra, respetivamente.
 * <p>
 * Os campos queryInfoName e queryInfoType referem-se, respetivamente, ao Nome e
 * Tipo da query que será enviada.
 * <p>
 * Esta classe também contem métodos que manipulam os seus elementos de forma
 * que as instâncias deste objeto possam
 * codificar e descodificar os elementos contidos nestas.
 * <p>
 * 11 novembro 2022
 * <p>
 * Rita Vaz
 * <p>
 * Robert Szabo
 */

public class DNSHandler {
    // Variáveis que constituem o Header do protocolo aplicacional.
    private int messageID;
    private String flags;
    private int responseCode;
    private int nValues;
    private int nAuthorities;
    private int nExtraValues;

    // Variáveis que constituem o Data do protocolo aplicacional.
    private String queryInfoName;
    private String queryInfoType;
    private String[] values;
    private String[] authorities;
    private String[] extra;

    // Construtor que cria estrutura protocular da query do cliente.
    public DNSHandler(int messageID, String name, String typeOfValue) {
        if (typeOfValue.toUpperCase().equals("PTR"))
            this.queryInfoName = reverseIP(name) + ".in-addr.reverse.";
        else
            this.queryInfoName = name;
        this.messageID = messageID;
        this.queryInfoType = typeOfValue.toUpperCase();
        this.flags = "Q";
    }

    // Construtor que dada um array de bytes descodifica para a estrutura protocolar
    // definida.
    public DNSHandler(byte[] buf) {
        String[] dados; // Variável auxiliar para guardar os dados do parsing do payload de um Datagrama
        String payload = new String(buf); // Conversão da mensagem em bytes para String
        dados = payload.split(","); // Divisão da mensagem por partes (undo do método toString)
        this.messageID = Integer.parseInt(dados[0]);
        this.flags = dados[1];
        this.responseCode = Integer.parseInt(dados[2]);
        this.nValues = Integer.parseInt(dados[3]);
        this.nAuthorities = Integer.parseInt(dados[4]);
        this.nExtraValues = Integer.parseInt(dados[5]);
        this.queryInfoName = dados[6];
        this.queryInfoType = dados[7];
        this.values = new String[nValues];
        if (nValues != 0)
            System.arraycopy(dados, 8, values, 0, nValues);
        this.authorities = new String[nAuthorities];
        if (nAuthorities != 0)
            System.arraycopy(dados, 8 + values.length, authorities, 0, nAuthorities);
        this.extra = new String[nExtraValues];
        if (nExtraValues != 0)
            System.arraycopy(dados, 8 + values.length + authorities.length, extra, 0, nExtraValues);
    }

    // Construtor que criará o objeto com todos os dados necessários já atribuidos.
    public DNSHandler(int messageID,
            String flags,
            int responseCode,
            int nValues,
            int nAuthorities,
            int nExtraValues,
            String queryInfoName,
            String queryInfoType,
            String[] values,
            String[] authorities,
            String[] extra) {
        this.messageID = messageID;
        this.flags = flags;
        this.responseCode = responseCode;
        this.nValues = nValues;
        this.nAuthorities = nAuthorities;
        this.nExtraValues = nExtraValues;
        this.queryInfoName = queryInfoName;
        this.queryInfoType = queryInfoType;
        this.values = values;
        this.authorities = authorities;
        this.extra = extra;
    }

    public int getMessageID() {
        return messageID;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public int getnValues() {
        return nValues;
    }

    public int getnAuthorities() {
        return nAuthorities;
    }

    public int getnExtraValues() {
        return nExtraValues;
    }

    public String getQueryInfoName() {
        return queryInfoName;
    }

    public String getQueryInfoType() {
        return queryInfoType;
    }

    public String[] getValues() {
        return values;
    }

    public String[] getAuthorities() {
        return authorities;
    }

    public String[] getExtra() {
        return extra;
    }

    public void printPDU() {
        int i, max;
        System.out.println("\n# Header");
        System.out.println("MESSAGE-ID = " + messageID +
                ", FLAGS = " + flags +
                ", RESPONSE-CODE = " + responseCode +
                ",\nN-VALUES = " + nValues +
                ", N-AUTHORITIES = " + nAuthorities +
                ", N-EXTRA-VALUES = " + nExtraValues + ";");
        System.out.println("\n# Data: Query Info");
        System.out.println(
                "QUERY-INFO.NAME = " + getQueryInfoName() +
                        ", QUERY-INFO.TYPE = " + queryInfoType + ";");
        System.out.println("\n# Data: List of Responses, Authorities and Extra Values");
        if (nValues != 0) {
            // for(String v: values) System.out.println("RESPONSE-VALUES = " + v);
            max = nValues - 1;
            for (i = 0; i < max; i++)
                System.out.println("RESPONSE-VALUES = " + values[i] + ",");
            System.out.println("RESPONSE-VALUES = " + values[i] + ";");
        } else
            System.out.println("RESPONSE-VALUES = (Null);");
        if (nAuthorities != 0) {
            // for(String v: authorities) System.out.println("AUTHORITIES-VALUES = " + v);
            max = nAuthorities - 1;
            for (i = 0; i < max; i++)
                System.out.println("AUTHORITIES-VALUES = " + authorities[i] + ",");
            System.out.println("AUTHORITIES-VALUES = " + authorities[i] + ";");
        } else
            System.out.println("AUTHORITIES-VALUES = (Null);");
        if (nExtraValues != 0) {
            // for(String v: extra) System.out.println("EXTRA-VALUES = " + v);
            max = nExtraValues - 1;
            for (i = 0; i < max; i++)
                System.out.println("EXTRA-VALUES = " + extra[i] + ",");
            System.out.println("EXTRA-VALUES = " + extra[i] + ";");
        } else
            System.out.println("EXTRA-VALUES = (Null);");
        System.out.println();
    }

    public String printPDUConciso() {

        StringBuilder responseV = new StringBuilder();
        StringBuilder authorityV = new StringBuilder();
        StringBuilder extraV = new StringBuilder();
        int i, max;

        if (nValues != 0) {
            max = nValues - 1;
            for (i = 0; i < max; i++)
                responseV.append("\n" + values[i] + ",");
            responseV.append("\n" + values[i] + ";");
        } else
            responseV.append("\n(Null);");
        if (nAuthorities != 0) {
            max = nAuthorities - 1;
            for (i = 0; i < max; i++)
                authorityV.append("\n" + authorities[i] + ",");
            authorityV.append("\n" + authorities[i] + ";");
        } else
            authorityV.append("\n(Null);");
        if (nExtraValues != 0) {
            max = nExtraValues - 1;
            for (i = 0; i < max; i++)
                extraV.append("\n" + extra[i] + ",");
            extraV.append("\n" + extra[i] + ";");
        } else
            extraV.append("\n(Null);");

        return "\n" + messageID +
                ", " + flags +
                ", " + responseCode +
                ", " + nValues +
                ", " + nAuthorities +
                ", " + nExtraValues +
                ";\n" + queryInfoName +
                ", " + queryInfoType +
                ";" + responseV
                + authorityV
                + extraV + "\n";

    }

    @Override
    public String toString() {
        StringBuilder responseV = new StringBuilder();
        if (values != null)
            for (String x : values)
                responseV.append(x).append(",");
        StringBuilder authorityV = new StringBuilder();
        if (authorities != null)
            for (String x : authorities)
                authorityV.append(x).append(",");
        StringBuilder extraV = new StringBuilder();
        if (extra != null)
            for (String x : extra)
                extraV.append(x).append(",");

        return messageID + "," +
                flags + "," +
                responseCode + "," +
                nValues + "," +
                nAuthorities + "," +
                nExtraValues + "," +
                queryInfoName + "," +
                queryInfoType + "," +
                responseV +
                authorityV +
                extraV;
    }

    public static String reverseIP(String str) {
        String part[] = str.split("\\.");
        return part[3]+"."+part[2]+"."+part[1]+"."+part[0];
    }


    public DNSHandler clone()
    {
        String[] v = new String[this.nValues];
        int i = 0;
        for(String s: this.getValues())
        {
            v[i]=String.valueOf(s);
            i++;
        }
        String[] a = new String[this.nAuthorities];
        i = 0;
        for(String s: this.getAuthorities())
        {
            a[i]=String.valueOf(s);
            i++;
        }
        String[] e = new String[nExtraValues];
        i = 0;
        for(String s: this.getExtra())
        {
            e[i]=String.valueOf(s);
            i++;
        }
            

        return new DNSHandler(this.getMessageID(),
            String.valueOf(this.getFlags()),
            this.getResponseCode(),
            this.getnValues(),
            this.getnAuthorities(),
            this.getnExtraValues(),
            String.valueOf(this.getQueryInfoName()),
            String.valueOf(this.getQueryInfoType()),
            v,
            a,
            e);
    }
}
