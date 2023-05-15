import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Data {

    // Cache.
    public DNSResourceRecord[] cache;

    private ReentrantReadWriteLock dbLock = new ReentrantReadWriteLock();

    // Determina o número máximo de linhas da cache
    public static Integer MAX = 50;

    // Momento em que ocorreu o arranque do servidor.
    LocalDateTime start;

    //private HashMap<String, String> alias = new HashMap<>();

    //public void setAlias(HashMap<String, String> alias) {
    //    this.alias = alias;
    //}

    //public HashMap<String, String> getAlias() {
    //    return alias;
    //}

    public Data(LocalDateTime start) throws IOException {

        this.start = start;
        // inicializa a cache
        this.cache = new DNSResourceRecord[MAX];
        int i;
        for (i = 0; i < MAX; i++)
            this.cache[i] = new DNSResourceRecord(
                    null,
                    null,
                    null,
                    0,
                    0,
                    null,
                    0,
                    0,
                    "FREE");

    }

    // Devolve a cache.
    public DNSResourceRecord[] getCache() {
        try {
            this.dbLock.readLock().lock();
            return this.cache;
        } finally {
            this.dbLock.readLock().unlock();
        }

    }

    // Guarda a cache.
    public void setCache(DNSResourceRecord[] cache) {
        this.dbLock.writeLock().lock();
        this.cache = cache;
        this.dbLock.writeLock().unlock();
    }

    /**
     * Método que constroi uma resposta do tipo DNSHandler para uma query relativa
     * a um Name e Type. Caso encontre matches com o Name e Type, adiciona as
     * respetivas
     * entradas da base de dados nos responseValues. Adiciona também as entradas
     * respetivas aos servidores autoritários de um domínio caso encontre matches
     * com o Name
     * e o Type = NS. Nos extraValues são adicionadas as entradas respetivas aos IPs
     * dos
     * servidores referidos em responseValues e authorityValues;
     **/
    public DNSHandler buildResponse(DNSHandler query) {
        // Variáveis para armazenar dados do Header do protocolo.
        int messageID = query.getMessageID();
        String flags = query.getFlags();
        int responseCode = 0;
        int nValues = 0;
        int nAuthorities = 0;
        int nExtras = 0;
        // Variáveis para armazenar dados de Data do protocolo.
        String queryInfoName = query.getQueryInfoName();
        String queryInfoType = query.getQueryInfoType();
        String[] responseValues = null;
        String[] authorityValues = null;
        String[] extraValues = null;

        // Estrutura para armazenar a resposta à query
        DNSHandler response;
        // Procura resposta para a query, flag Q ativa indica ser uma query.
        if (flags.contains("Q")) {
            // Na resposta a uma query a flag Q é desativada.
            // Nesta fase, assume-se que a query é feita a um servidor que é autoritativo
            // para o dominio indicado na query
            flags = "A";
            // Armazenar resultado da pesquisa dos matches na base de dados.
            Map<String, List<Integer>> responses = searchCache(1, queryInfoName, queryInfoType);

            nValues = responses.get("responseValues").size();
            nAuthorities = responses.get("authorityValues").size();
            nExtras = responses.get("extraValues").size();

            // Verifica número de respostas para o match Name e Type
            // Se não houver respostas para este Name e Type o campo responseValues fica
            // vazio.
            if (nValues > 0) {
                responseValues = new String[nValues];
                int pos = 0;
                for (int i : responses.get("responseValues"))
                    responseValues[pos++] = this.cache[i].imprimeLinha();
            } else {
                if (nAuthorities > 0)
                    responseCode = 1;
                else
                    responseCode = 2;
            }

            // Verifica número de respostas para o match Name e Type "NS"
            // Se não houver respostas para este Name e Type "NS" o campo authorityValues
            // fica vazio.
            if (responseCode != 2) {
                if (nAuthorities > 0) {
                    authorityValues = new String[nAuthorities];
                    int pos = 0;
                    for (int i : responses.get("authorityValues"))
                        authorityValues[pos++] = this.cache[i].imprimeLinha();
                }
                // Caso para o CNAME
                else{
                    nAuthorities = responses.get("authorityValues2").size();
                    authorityValues = new String[nAuthorities];
                    int pos = 0;
                    for (int i : responses.get("authorityValues2"))
                        authorityValues[pos++] = this.cache[i].imprimeLinha();
                }

                // Verifica número de respostas para o match Name e Type "A"
                // Se não houver respostas para este Name e Type "A" o campo extraValues fica
                // vazio.
                if (nExtras > 0) {
                    extraValues = new String[nExtras];
                    int pos = 0;
                    for (int i : responses.get("extraValues"))
                        extraValues[pos++] = this.cache[i].imprimeLinha();
                }
                // Caso para o CNAME
                else {
                    nExtras = responses.get("extraValues2").size();
                    extraValues = new String[nExtras];
                    int pos = 0;
                    for (int i : responses.get("extraValues2"))
                        extraValues[pos++] = this.cache[i].imprimeLinha();
                }
            } else {
                nAuthorities = responses.get("authorityValues2").size();
                nExtras = responses.get("extraValues2").size();

                if (nAuthorities > 0) {
                    authorityValues = new String[nAuthorities];
                    int pos = 0;
                    for (int i : responses.get("authorityValues2"))
                        authorityValues[pos++] = this.cache[i].imprimeLinha();
                }

                if (nExtras > 0) {
                    extraValues = new String[nExtras];
                    int pos = 0;
                    for (int i : responses.get("extraValues2"))
                        extraValues[pos++] = this.cache[i].imprimeLinha();
                }
            }

            // Constroi a resposta à query com os novos dados
            response = new DNSHandler(
                    messageID,
                    flags,
                    responseCode,
                    nValues,
                    nAuthorities,
                    nExtras,
                    queryInfoName,
                    queryInfoType,
                    responseValues,
                    authorityValues,
                    extraValues);
        }
        // Se a flag Q não está ativa, a mensagem não está codificada corretamente como
        // uma query.
        else {
            responseCode = 3;
            flags = "A";
            response = new DNSHandler(
                    messageID,
                    flags,
                    responseCode,
                    nValues,
                    nAuthorities,
                    nExtras,
                    queryInfoName,
                    queryInfoType,
                    null,
                    null,
                    null);
        }
        return response;
    }


    public DNSHandler buildResponse2(DNSHandler query) {
        // Variáveis para armazenar dados do Header do protocolo.
        int messageID = query.getMessageID();
        String flags = query.getFlags();
        int responseCode = 0;
        int nValues = 0;
        int nAuthorities = 0;
        int nExtras = 0;
        // Variáveis para armazenar dados de Data do protocolo.
        String queryInfoName = query.getQueryInfoName();
        String queryInfoType = query.getQueryInfoType();
        String[] responseValues = null;
        String[] authorityValues = null;
        String[] extraValues = null;

        // Estrutura para armazenar a resposta à query
        DNSHandler response;
        // Procura resposta para a query, flag Q ativa indica ser uma query.
        
        flags = "";

        // Armazenar resultado da pesquisa dos matches na base de dados.
        Map<String, List<Integer>> responses = searchCache2(1, queryInfoName, queryInfoType);

        nValues = responses.get("responseValues").size();
        nAuthorities = responses.get("authorityValues").size();
        nExtras = responses.get("extraValues").size();
        
        responseValues = new String[nValues];
        int pos = 0;
        for (int i : responses.get("responseValues"))
            responseValues[pos++] = this.cache[i].imprimeLinha();

        authorityValues = new String[nAuthorities];
        pos = 0;
        for (int i : responses.get("authorityValues"))
            authorityValues[pos++] = this.cache[i].imprimeLinha();

        extraValues = new String[nExtras];
        pos = 0;
        for (int i : responses.get("extraValues"))
            extraValues[pos++] = this.cache[i].imprimeLinha();


            // Constroi a resposta à query com os novos dados
        return response = new DNSHandler(
                    messageID,
                    flags,
                    responseCode,
                    nValues,
                    nAuthorities,
                    nExtras,
                    queryInfoName,
                    queryInfoType,
                    responseValues,
                    authorityValues,
                    extraValues);
        
       
    }

    /**
     * Método que faz a união lógica de duas listas, contendo todos os elementos não
     * repetidos das duas listas.
     * Retorna uma lista com esses elementos.
     */
    public <T> List<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    }

    /**
     * Método que recebe uma lista de Strings e devolve uma String com o prefixo
     * comum mais longo encontrado entre todas as Strings.
     */
    public String lpm(String[] a) {
        int size = a.length;
        /* Se o tamanho é 0, devolve uma String vazia "" */
        if (size == 0)
            return "";
        if (size == 1)
            return a[0];
        /* Ordena o array de Strings */
        Arrays.sort(a);
        /* Encontra o tamanho mínimo da primeira e última String */
        int end = Math.min(a[0].length(), a[size - 1].length());
        /* Econtra o prefixo comum entre a primeira e última String */
        int i = 0;
        while (i < end && a[0].charAt(i) == a[size - 1].charAt(i))
            i++;
        String pre = a[0].substring(0, i);
        return pre;
    }

    public static String reverseString(String str) {
        StringBuilder sb = new StringBuilder(str);
        sb.reverse();
        return sb.toString();
    }



    /**
     * Verifica em todas as linhas da cache, se aquela informação já existe, 
     * tendo em conta que são apenas considerados os parâmetros nome, tipo e valor para a equivalência.
     */
    public boolean checkIfLineExists(DNSResourceRecord newRecord)
    {
        int i;
        boolean r = false;
        for(i=0;!r && i<MAX;i++)
        {
            if(!this.cache[i].getStatus().equals("FREE"))
            {
                if(newRecord.equalsRec(this.cache[i])==true)
                    r=true;
            }
            
        }
        
        return r;
    }


    /**
     * Método que adiciona na cache as respostas recebidas.
     * Recebe como argumento a resposta em formato DNSHandler.
     **/
    public void addResponseToCache(DNSHandler response) {
        int i = 0, j;
        DNSResourceRecord rec;

        // percorre as linhas da cache até obter uma posição livre
        while (!this.cache[i].getStatus().equals("FREE"))
            i++;
        // index i é uma posição livre da cache
        
        for (j = 0; j < response.getnValues(); j++) {
            // guarda a linha na cache
            rec = new DNSResourceRecord(response.getValues()[j], j,
                    (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start));
            // se o record ainda não existir, adiciona-o
            if(!checkIfLineExists(rec))
                this.cache[i] = rec;
            // percorre as linhas da cache até obter uma posição livre
            while (!this.cache[i].getStatus().equals("FREE"))
                i++;
        }

        for (j = 0; j < response.getnAuthorities(); j++) {
            // guarda a linha na cache
            rec = new DNSResourceRecord(response.getAuthorities()[j], j,
                    (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start));
            // se o record ainda não existir, adiciona-o
            if(!checkIfLineExists(rec))
                this.cache[i] = rec;
                
            // percorre as linhas da cache até obter uma posição livre
            while (!this.cache[i].getStatus().equals("FREE"))
                i++;
        }

        for (j = 0; j < response.getnExtraValues(); j++) {
            // guarda a linha na cache
            rec = new DNSResourceRecord(response.getExtra()[j], j,
                    (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start));
            // se o record ainda não existir, adiciona-o
            if(!checkIfLineExists(rec))
            {
                this.cache[i] = rec;
            }
            // percorre as linhas da cache até obter uma posição livre
            while (!this.cache[i].getStatus().equals("FREE"))
                i++;
        }
    }

    /**
     * Método que procura na cache as respostas para a query do cliente.
     * Recebe como argumentos o index, Name e Type.
     * A procura começa a partir da entrada com o índice Index.
     * O método devolve o índice da primeira entrada encontrada que faça o match com
     * Name e Type, caso o index diferir de 1.
     * A procura devolve um Map caso o index for 1. O Map tem como chaves as strings
     * "responseValues", "authorityValues", "extraValues".
     * Cada chave do Map tem como valor uma lista de inteiros, sendo que cada
     * inteiro corresponde ao index de uma linha de cache.
     **/
    public Map<String, List<Integer>> searchCache(Integer index, String nameQ, String type) {
        // Criação do Map que será devolvido como resposta ao método.
        Map<String, List<Integer>> res = new HashMap<>();
        res.put("responseValues", new ArrayList<>());
        res.put("authorityValues", new ArrayList<>());
        res.put("extraValues", new ArrayList<>());
        String dominio = "";
        String name = nameQ;

        // Procura ocorrências do uso de alias na query.
        // for (Object x : alias.keySet().toArray())
        //     if (name.contains((String) x))
        //         name = name.replace((String) x, alias.get(x));

        // A procura começa a partir da posição definida pelo index-1.
        int i;
        /*
         * Boleano que indica se o método devolve apenas 1 resultado.
         * Se for verdadeiro então devolve a hash com apenas o índice da primeira
         * entrada encontrada que faça o match com Name e Type.
         */
        boolean one = false;

        // Boleano que indica se já foi encontrado pelo menos um valor.
        boolean find = false;

        this.dbLock.readLock().lock();
        // Pesquisa por todas as linhas da cache ou, no caso de se pretender apenas o
        // primeiro resultado, até encontrá-lo.
        for (i = index - 1; i < this.cache.length && !one; i++) {
            // Se o TTL da entrada for menor do que o tempo entre o timestamp e o final, a
            // entrada da cache fica livre.
            if (this.cache[i].getttl() < (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start)
                    - this.cache[i].getTimeStamp()) {
                this.dbLock.writeLock().lock();
                this.cache[i].setStatus("FREE");
                this.dbLock.writeLock().unlock();
            }

            if (!this.cache[i].getStatus().equals("FREE")) {

                if (this.cache[i].getName().equals("@") && this.cache[i].getType().equals("DEFAULT")) {
                    dominio = this.cache[i].getValue();
                }
                // É calculado o maior sufixo comum para procurar informação sobre algum
                // possível domínio que tenha respostas
                String[] cmp = { reverseString(this.cache[i].getName()), reverseString(nameQ) };
                String lsm = reverseString(lpm(cmp));
                if (lsm.length() <= dominio.length())
                    lsm = "";
                /*
                 * É acrescentada à lista de posições de response values se os nomes e tipos
                 * corresponderem.
                 * if (type.equals("CNAME"))
                 * if (Objects.equals(this.cache[i].getValue(), nameQ)
                 * && Objects.equals(this.cache[i].getType(), "CNAME")) {
                 * res.get("responseValues").add(i);
                 * find = true;
                 * }
                 */

                if(Objects.equals(type,"A") && Objects.equals(this.cache[i].getType(),"A") && Objects.equals(this.cache[i].getName(), name))
                    res.get("responseValues").add(i);

                else if(Objects.equals(type,"CNAME") && Objects.equals(this.cache[i].getValue(),nameQ))
                {
                    res.get("responseValues").add(i);
                    find = true;
                }
                else if ((Objects.equals(this.cache[i].getName(), name) || Objects.equals(this.cache[i].getName(), nameQ))
                        && Objects.equals(this.cache[i].getType(), type)) {
                    if(!this.cache[i].getValue().contains("sp")) res.get("responseValues").add(i);
                    find = true;
                }

                // É acrescentada à lista de posições de authorities values se os nomes e tipos
                // corresponderem e se o tipo for NS.
                if ((Objects.equals(this.cache[i].getName(), name) || this.cache[i].getName().equals(lsm))
                        && Objects.equals(this.cache[i].getType(), "NS")) {
                    res.get("authorityValues").add(i);
                }

                /*
                 * É acrescentada à lista de posições de extraValues se for uma entrada do tipo
                 * A e fizer match no parâmetro com todos
                 * os valores no campo RESPONSE VALUES e no campo AUTHORITIES VALUES.
                 */
                if ((find || res.get("authorityValues").size() > 0) && Objects.equals(this.cache[i].getType(), "A")) {
                    List<Integer> aux = union(res.get("responseValues"), res.get("authorityValues"));

                    /**
                     * Verifica se o valor da linha em cache de x é igual ao nome da linha que
                     * estamos a pesquisar agora tanto para as
                     * posições da cache que correspondem a response values como authorities values.
                     **/
                    for (Integer x : aux)
                        if (Objects.equals(this.cache[i].getName(), this.cache[x].getValue()))
                            res.get("extraValues").add(i);
                }
            }
            // Se o index for diferente de 1 e já foi encontrado pelo menos 1 resultado,
            // coloca-se o boleano one a true, terminando o ciclo.
            if (index != 1 && find)
                one = true;
        }
        // No caso de não encontrar match (responseCode == 2), adiciona os servidores
        // autoritativos e os respetivos IPs
        // a authorityValues2 e extraValues2
        if (find == false || ((type.equals("CNAME") || type.equals("A") || type.equals("PTR")) && res.get("responseValues").size() != 0)) {
            res.put("authorityValues2", new ArrayList<>());
            res.put("extraValues2", new ArrayList<>());

            for (i = index - 1; i < this.cache.length && !one; i++) {
                if (!this.cache[i].getStatus().equals("FREE")) {
                    if (this.cache[i].getName().equals("@") && this.cache[i].getType().equals("DEFAULT"))
                        dominio = this.cache[i].getValue();

                    // É acrescentada à lista de posições de authorities values se os nomes e tipos
                    // corresponderem e se o tipo for NS.
                    if (Objects.equals(this.cache[i].getName(), dominio)
                            && Objects.equals(this.cache[i].getType(), "NS")) {
                        res.get("authorityValues2").add(i);
                        find = true;
                    }

                    /*
                     * É acrescentada à lista de posições de extraValues2 se for uma entrada do tipo
                     * A e fizer match no parâmetro com todos
                     * os valores no campo RESPONSE VALUES e no campo AUTHORITIES VALUES.
                     */
                    if (res.get("authorityValues2").size() != 0 && Objects.equals(this.cache[i].getType(), "A")) {
                        /**
                         * Verifica se o valor da linha em cache de x é igual ao nome da linha que
                         * estamos a pesquisar agora para as posições da cache que correspondem a
                         * authorities values.
                         **/
                        for (Integer x : res.get("authorityValues2"))
                            if (Objects.equals(this.cache[i].getName(), this.cache[x].getValue()))
                                res.get("extraValues2").add(i);

                        find = true;
                    }
                }
                // Se o index for diferente de 1 e já foi encontrado pelo menos 1 resultado,
                // coloca-se o boleano one a true, terminando o ciclo.
                if (index != 1 && find)
                    one = true;
            }
            
            if(type.equals("CNAME"))
            {
                List<Integer> n = res.get("responseValues");
                List<Integer> change = new ArrayList<>();
                for(i=0;i<n.size();i++)
                {
                    int s = (int) n.get(i);
                    if(this.cache[s].getType().equals("CNAME"))
                    {
                        change.add(s);
                    }
                }
                res.put("responseValues",change);
            }

        }
        this.dbLock.readLock().unlock();
        return res;

    }

    /**
     * Método que separa por linhas o ficheiro.
     * Recebe o caminho para um ficheiro.
     * Retorna uma lista de linhas.
     **/
    public List<String> readFile(String nameFile) throws FileNotFoundException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(nameFile), StandardCharsets.UTF_8);
        } catch (IOException exc) {
            lines = new ArrayList<>();
        }
        if (lines.isEmpty())
            throw new FileNotFoundException("Ficheiro não encontrado");
        return lines;
    }


    public Map<String, List<Integer>> searchCache2(Integer index, String nameQ, String type) {
        // Criação do Map que será devolvido como resposta ao método.
        Map<String, List<Integer>> res = new HashMap<>();
        res.put("responseValues", new ArrayList<>());
        res.put("authorityValues", new ArrayList<>());
        res.put("extraValues", new ArrayList<>());
        String dominio = "";
        String name = nameQ;

        // Procura ocorrências do uso de alias na query.
        // for (Object x : alias.keySet().toArray())
        //     if (name.contains((String) x))
        //         name = name.replace((String) x, alias.get(x));

        // A procura começa a partir da posição definida pelo index-1.
        int i;
        
        this.dbLock.readLock().lock();

        // Pesquisa por todas as linhas da cache ou, no caso de se pretender apenas o
        // primeiro resultado, até encontrá-lo.
        for (i = index - 1; i < this.cache.length; i++) {
            // Se o TTL da entrada for menor do que o tempo entre o timestamp e o final, a
            // entrada da cache fica livre.
            if (this.cache[i].getttl() < (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start)
                    - this.cache[i].getTimeStamp()) {
                this.dbLock.writeLock().lock();
                this.cache[i].setStatus("FREE");
                this.dbLock.writeLock().unlock();
            }

            if (!this.cache[i].getStatus().equals("FREE")) {

                
                if (Objects.equals(this.cache[i].getName(), nameQ) && Objects.equals(this.cache[i].getType(), type)) {
                    if(!this.cache[i].getValue().contains("sp")) res.get("responseValues").add(i);
                }

            }
        }

        List rv = res.get("responseValues");
        int size = rv.size();
        int pos;


        for (i = index - 1; i < this.cache.length; i++) {
            
            if (!this.cache[i].getStatus().equals("FREE")) {

                for(pos=0;pos<size;pos++)
                {
                    if (!res.get("authorityValues").contains(i) && Objects.equals(this.cache[i].getType(), "NS") 
                        && ( Objects.equals(this.cache[i].getName(), this.cache[(int)rv.get(pos)].getName()) ))
                            res.get("authorityValues").add(i);

                    if (!res.get("extraValues").contains(i) && Objects.equals(this.cache[i].getType(), "A") && Objects.equals(this.cache[i].getName(), this.cache[(int)rv.get(pos)].getValue()))
                                res.get("extraValues").add(i);
                }

                
            }
        }


        List ra = res.get("authorityValues");
        int sizeA = ra.size();

        for (i = index - 1; i < this.cache.length; i++) {
            
            if (!this.cache[i].getStatus().equals("FREE")) {

                for(pos=0;pos<sizeA;pos++)
                {
                    if (!res.get("extraValues").contains(i) && Objects.equals(this.cache[i].getType(), "A") && 
                    Objects.equals(this.cache[i].getName(), this.cache[(int)ra.get(pos)].getValue()))
                            res.get("extraValues").add(i);
                }
            }
        }


        this.dbLock.readLock().unlock();
        return res;

    }


    /**
     * Recebe um ficheiro de cofiguração.
     * Retorna a instância Dados criada considerando os valores do ficheiro
     * recebido.
     **/
    public int parserDados(String filename) throws FileNotFoundException {
        List<String> lines = readFile(filename);
        String defaultDom = "";
        int defaultTTL = 0, i = 0;
        // Lê linha a linha, a partir da lista linhas criada através do método readFile.
        for (String line : lines) {

            // Divide cada linha em partes através do método split, sendo que, usa como
            // método de divisão o espaço.
            String[] part = line.split(" ");

            /**
             * Cada linha fica com as partes divididas da seguinte forma:
             * parte[0] = Parâmetro
             * parte[1] = Tipo de valor
             * parte[2] = Valor // guardado sempre como string
             * parte[3] = Tempo de Validade (TTL)
             * parte[4] = Prioridade
             **/

            /**
             * Apenas guarda informações se a linha não começar com '#', uma vez que este
             * simbolo indica que tudo o que
             * aparece à frente são comentários.
             * Adiciona os valores nas respetivas variáveis de instância, consoante o tipo
             * de valor (part[1]).
             **/
            DNSResourceRecord rec;
            if (!(part[0].equals("#")) && !(part[0].equals(""))) {
                if (part[0].equals("@") && part[1].equals("DEFAULT")) {
                    // Define o valor predefinido do domínio
                    defaultDom = part[2];
                    if (this.cache[i].getStatus().equals("FREE")) {
                        rec = new DNSResourceRecord(part[0], part[1], part[2], 0, 0, "FILE", (i + 1),
                                (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start), "VALID");
                        this.cache[i++] = rec;
                    }
                } else if (part[0].equals("TTL") && part[1].equals("DEFAULT")) {
                    // Define o predefinido do TTL
                    defaultTTL = Integer.parseInt(part[2]);
                } else {
                    // Substitui ocorrencias de @ pelo domínio predefinido.
                    if (part[0].contains("@"))
                        part[0] = part[0].replace("@", defaultDom);
                    // Acrescenta o sufixo (domínio predefinido) aos nomes que não estejam completos
                    // menos às linhas CNAME
                    else if (!(part[1].equals("CNAME")))
                        part[0] = part[0].concat(defaultDom);
                    else {
                        part[0] = part[0].concat(defaultDom);
                        part[2] = part[2].concat(defaultDom);
                        //this.getAlias().put(part[0], part[2]);
                    }
                    if (part.length > 3 && part[3].equals("TTL"))
                        part[3] = String.valueOf(defaultTTL);
                    // Como as entradas na cache são escritas para o final da lista, a probabilidade
                    // do tempo de validade das
                    // primeiras entradas, não referentes aos ficheiros e, por isso, com status
                    // FREE, serem vazias é alta
                    if (this.cache[i].getStatus().equals("FREE")) {
                        if (part.length == 3)
                            rec = new DNSResourceRecord(part[0], part[1], part[2], 0, 0, "FILE", (i + 1),
                                    (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start), "VALID");
                        else if (part.length == 4)
                            rec = new DNSResourceRecord(part[0], part[1], part[2], Integer.valueOf(part[3]), 0, "FILE",
                                    (i + 1), (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start),
                                    "VALID");
                        else
                            rec = new DNSResourceRecord(part[0], part[1], part[2], Integer.valueOf(part[3]),
                                    Integer.valueOf(part[4]), "FILE", (i + 1),
                                    (int) ChronoUnit.SECONDS.between(LocalDateTime.now(), this.start), "VALID");

                        this.cache[i] = rec;
                        i++;
                    }
                }
            }
        }
        return i;
    }

}