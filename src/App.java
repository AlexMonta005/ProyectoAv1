import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.border.*;

public class App {
    private static final Color NAVY = new Color(20, 42, 68);
    private static final Color TEAL = new Color(25, 112, 103);
    private static final Color BG = new Color(244, 247, 250);
    private static final Color TEXT = new Color(38, 52, 68);

    private final JFrame frame = new JFrame("Sistema de información - EDA II");
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private JComponent currentDetail;

    private final Map<String, Topic> topics = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 14));
        registerTopics();

        root.add(createCover(), "cover");
        root.add(createIndex(), "index");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.setSize(1050, 700);
        frame.setMinimumSize(new Dimension(850, 600));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ------------------------------------------------------------------
    //  Registro de temas y ejemplos ejecutables
    // ------------------------------------------------------------------
    private void registerTopics() {
        topics.put("Eficiencia algorítmica", new Topic(
                "Eficiencia algorítmica",
                "Estudia cuánto tiempo y recursos necesita un algoritmo para resolver un problema.",
                new String[]{"Comparación de soluciones", "Conteo de operaciones", "Tiempo de ejecución"},
                new Example[]{
                        new Example("Búsqueda lineal vs. binaria",
                                "Entrada: «n objetivo». Busca el objetivo en el arreglo ordenado 1..n y cuenta las comparaciones.",
                                "1000 777", App::searchCompare)
                }));
        topics.put("Análisis de complejidad", new Topic(
                "Análisis de complejidad",
                "Permite describir cómo crecen el tiempo y el espacio utilizados cuando aumenta el tamaño de la entrada.",
                new String[]{"Notación Big-O", "Mejor, promedio y peor caso", "Complejidad temporal y espacial"},
                new Example[]{
                        new Example("Crecimiento de operaciones",
                                "Entrada: tamaños n separados por espacios. Muestra cuántas operaciones requiere cada orden de complejidad.",
                                "5 10 20 30", App::growthTable)
                }));
        topics.put("P, NP, NP-Hard y NP-Completo", new Topic(
                "P, NP, NP-Hard y NP-Completo",
                "Clasifica problemas según la dificultad de resolverlos o verificar una solución.",
                new String[]{"Clase P", "Clase NP", "Problemas NP-Hard", "Problemas NP-Completo"},
                new Example[]{
                        new Example("Suma de subconjuntos (NP-Completo)",
                                "Línea 1: números. Línea 2: objetivo. Se busca por fuerza bruta (exponencial) y luego se verifica el certificado (polinomial).",
                                "3 34 4 12 5 2\n9", App::subsetSum)
                }));
        topics.put("Grafos", new Topic(
                "Grafos",
                "Representa problemas mediante vértices y aristas para estudiar conexiones, recorridos y rutas.",
                new String[]{"Ordenamiento topológico (Kahn)", "TSM", "Kruskal", "Prim", "Dijkstra"},
                new Example[]{
                        new Example("Kahn (orden topológico)",
                                "Una arista dirigida por línea: «origen destino». Ejemplo: A B significa A -> B.",
                                "A C\nB C\nC D\nB E\nD F\nE F", App::kahn),
                        new Example("TSM",
                                "Una arista por línea: «u v peso» (grafo no dirigido). Busca el recorrido de menor costo que visita todos los vértices y regresa al inicio (se parte del primer vértice).",
                                "A B 10\nA C 15\nA D 20\nB C 35\nB D 25\nC D 30", App::tsm),
                        new Example("Kruskal",
                                "Una arista por línea: «u v peso» (grafo no dirigido).",
                                "A B 4\nA C 3\nB C 1\nB D 2\nC D 4\nC E 6\nD E 5", App::kruskal),
                        new Example("Prim",
                                "Una arista por línea: «u v peso». Opcional: «inicio X» para elegir el vértice inicial.",
                                "inicio A\nA B 4\nA C 3\nB C 1\nB D 2\nC D 4\nC E 6\nD E 5", App::prim),
                        new Example("Dijkstra",
                                "Una arista por línea: «u v peso» (no dirigido). Opcional: «inicio X» para el origen.",
                                "inicio A\nA B 7\nA C 9\nA F 14\nB C 10\nB D 15\nC D 11\nC F 2\nD E 6\nE F 9", App::dijkstra)
                }));
    }

    // ------------------------------------------------------------------
    //  Algoritmos de los ejemplos
    // ------------------------------------------------------------------
    private static class Edge {
        final String u, v; final int w;
        Edge(String u, String v, int w) { this.u = u; this.v = v; this.w = w; }
    }

    /** Lee aristas "u v [peso]". Devuelve las aristas y deja el vértice inicial en start[0] si hay línea "inicio X". */
    private static List<Edge> parseEdges(String input, boolean weighted, String[] start) {
        List<Edge> edges = new ArrayList<>();
        for (String raw : input.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if (p[0].equalsIgnoreCase("inicio") && p.length >= 2) {
                if (start != null) start[0] = p[1];
                continue;
            }
            if (p.length < (weighted ? 3 : 2))
                throw new IllegalArgumentException("Línea inválida: \"" + line + "\"");
            int w = 1;
            if (weighted) {
                try { w = Integer.parseInt(p[2]); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Peso inválido en: \"" + line + "\""); }
            }
            edges.add(new Edge(p[0], p[1], w));
        }
        if (edges.isEmpty()) throw new IllegalArgumentException("Ingresa al menos una arista.");
        return edges;
    }

    private static List<String> vertices(List<Edge> edges) {
        Set<String> s = new LinkedHashSet<>();
        for (Edge e : edges) { s.add(e.u); s.add(e.v); }
        return new ArrayList<>(s);
    }

    static String kahn(String input) {
        List<Edge> edges = parseEdges(input, false, null);
        List<String> vs = vertices(edges);
        Map<String, Integer> indeg = new LinkedHashMap<>();
        Map<String, List<String>> adj = new LinkedHashMap<>();
        for (String v : vs) { indeg.put(v, 0); adj.put(v, new ArrayList<>()); }
        for (Edge e : edges) { adj.get(e.u).add(e.v); indeg.merge(e.v, 1, Integer::sum); }

        StringBuilder sb = new StringBuilder("Grados de entrada iniciales: " + indeg + "\n\n");
        Deque<String> queue = new ArrayDeque<>();
        for (String v : vs) if (indeg.get(v) == 0) queue.add(v);
        List<String> order = new ArrayList<>();
        int step = 1;
        while (!queue.isEmpty()) {
            String u = queue.poll();
            order.add(u);
            sb.append("Paso ").append(step++).append(": se extrae ").append(u).append("\n");
            for (String v : adj.get(u)) {
                int d = indeg.merge(v, -1, Integer::sum);
                sb.append("    se elimina ").append(u).append(" -> ").append(v)
                  .append(" (grado de ").append(v).append(" = ").append(d).append(")\n");
                if (d == 0) queue.add(v);
            }
        }
        if (order.size() != vs.size())
            sb.append("\nEl grafo tiene al menos un ciclo: no existe orden topológico.");
        else
            sb.append("\nOrden topológico: ").append(String.join(" -> ", order));
        return sb.toString();
    }

    static String tsm(String input) {
        List<Edge> edges = parseEdges(input, true, null);
        List<String> vs = vertices(edges);
        int n = vs.size();
        if (n > 9) throw new IllegalArgumentException("Para este ejemplo usa a lo más 9 vértices (fuerza bruta).");
        final int INF = Integer.MAX_VALUE / 4;
        int[][] d = new int[n][n];
        for (int[] row : d) Arrays.fill(row, INF);
        for (Edge e : edges) {
            int a = vs.indexOf(e.u), b = vs.indexOf(e.v);
            d[a][b] = Math.min(d[a][b], e.w);
            d[b][a] = Math.min(d[b][a], e.w);
        }
        if (n < 2) throw new IllegalArgumentException("Se necesitan al menos 2 vértices.");
        int[] perm = new int[n - 1];
        for (int i = 0; i < n - 1; i++) perm[i] = i + 1;
        int[] best = null; int bestCost = INF; long tours = 0;
        do {
            tours++;
            int cost = 0, prev = 0; boolean ok = true;
            for (int x : perm) {
                if (d[prev][x] >= INF) { ok = false; break; }
                cost += d[prev][x]; prev = x;
            }
            if (ok && d[prev][0] < INF) cost += d[prev][0]; else ok = false;
            if (ok && cost < bestCost) { bestCost = cost; best = perm.clone(); }
        } while (nextPermutation(perm));

        StringBuilder sb = new StringBuilder("Vértices: " + vs + "\nRecorridos evaluados: " + tours + " (= (n-1)!)\n\n");
        if (best == null) return sb.append("No existe un ciclo que visite todos los vértices.").toString();
        sb.append("Mejor recorrido: ").append(vs.get(0));
        for (int x : best) sb.append(" -> ").append(vs.get(x));
        sb.append(" -> ").append(vs.get(0)).append("\nCosto total: ").append(bestCost);
        return sb.toString();
    }

    private static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        for (int l = i + 1, r = a.length - 1; l < r; l++, r--) { t = a[l]; a[l] = a[r]; a[r] = t; }
        return true;
    }

    private static String root(Map<String, String> parent, String x) {
        while (!parent.get(x).equals(x)) { parent.put(x, parent.get(parent.get(x))); x = parent.get(x); }
        return x;
    }

    static String kruskal(String input) {
        List<Edge> edges = parseEdges(input, true, null);
        List<String> vs = vertices(edges);
        Map<String, String> parent = new HashMap<>();
        for (String v : vs) parent.put(v, v);
        List<Edge> sorted = new ArrayList<>(edges);
        sorted.sort(Comparator.comparingInt(e -> e.w));
        StringBuilder sb = new StringBuilder("Aristas ordenadas por peso:\n");
        int total = 0, used = 0;
        for (Edge e : sorted) sb.append("  ").append(e.u).append("-").append(e.v).append(" (").append(e.w).append(")\n");
        sb.append("\n");
        for (Edge e : sorted) {
            String a = root(parent, e.u), b = root(parent, e.v);
            if (!a.equals(b)) {
                parent.put(a, b); total += e.w; used++;
                sb.append("Se acepta  ").append(e.u).append("-").append(e.v).append(" (").append(e.w).append(")\n");
            } else {
                sb.append("Se rechaza ").append(e.u).append("-").append(e.v).append(" (forma ciclo)\n");
            }
        }
        sb.append("\nPeso total: ").append(total);
        if (used < vs.size() - 1) sb.append("\n(El grafo no es conexo: se obtiene un bosque.)");
        return sb.toString();
    }

    static String prim(String input) {
        String[] st = {null};
        List<Edge> edges = parseEdges(input, true, st);
        List<String> vs = vertices(edges);
        String s = st[0] != null ? st[0] : vs.get(0);
        if (!vs.contains(s)) throw new IllegalArgumentException("El vértice inicial \"" + s + "\" no existe.");
        Map<String, List<Edge>> adj = new HashMap<>();
        for (String v : vs) adj.put(v, new ArrayList<>());
        for (Edge e : edges) { adj.get(e.u).add(e); adj.get(e.v).add(new Edge(e.v, e.u, e.w)); }

        Set<String> in = new LinkedHashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.w));
        in.add(s); pq.addAll(adj.get(s));
        StringBuilder sb = new StringBuilder("Vértice inicial: " + s + "\n\n");
        int total = 0;
        while (!pq.isEmpty() && in.size() < vs.size()) {
            Edge e = pq.poll();
            if (in.contains(e.v)) continue;
            in.add(e.v); total += e.w;
            sb.append("Se agrega ").append(e.u).append("-").append(e.v).append(" (").append(e.w).append(")\n");
            pq.addAll(adj.get(e.v));
        }
        sb.append("\nPeso total: ").append(total);
        if (in.size() < vs.size()) sb.append("\n(El grafo no es conexo: solo se alcanzó ").append(in).append(".)");
        return sb.toString();
    }

    static String dijkstra(String input) {
        String[] st = {null};
        List<Edge> edges = parseEdges(input, true, st);
        List<String> vs = vertices(edges);
        String s = st[0] != null ? st[0] : vs.get(0);
        if (!vs.contains(s)) throw new IllegalArgumentException("El vértice inicial \"" + s + "\" no existe.");
        for (Edge e : edges) if (e.w < 0) throw new IllegalArgumentException("Dijkstra no admite pesos negativos.");
        Map<String, List<Edge>> adj = new HashMap<>();
        for (String v : vs) adj.put(v, new ArrayList<>());
        for (Edge e : edges) { adj.get(e.u).add(e); adj.get(e.v).add(new Edge(e.v, e.u, e.w)); }

        Map<String, Integer> dist = new LinkedHashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String v : vs) dist.put(v, Integer.MAX_VALUE);
        dist.put(s, 0);
        PriorityQueue<Object[]> pq = new PriorityQueue<>(Comparator.comparingInt(o -> (Integer) o[1]));
        pq.add(new Object[]{s, 0});
        Set<String> done = new HashSet<>();
        while (!pq.isEmpty()) {
            Object[] top = pq.poll();
            String u = (String) top[0];
            if (!done.add(u)) continue;
            for (Edge e : adj.get(u)) {
                int nd = dist.get(u) + e.w;
                if (nd < dist.get(e.v)) { dist.put(e.v, nd); prev.put(e.v, u); pq.add(new Object[]{e.v, nd}); }
            }
        }
        StringBuilder sb = new StringBuilder("Origen: " + s + "\n\n");
        for (String v : vs) {
            sb.append("  ").append(s).append(" -> ").append(v).append(" : ");
            if (dist.get(v) == Integer.MAX_VALUE) { sb.append("inalcanzable\n"); continue; }
            LinkedList<String> path = new LinkedList<>();
            for (String x = v; x != null; x = prev.get(x)) path.addFirst(x);
            sb.append(dist.get(v)).append("   ruta: ").append(String.join(" -> ", path)).append("\n");
        }
        return sb.toString();
    }

    static String searchCompare(String input) {
        String[] p = input.trim().split("\\s+");
        if (p.length < 2) throw new IllegalArgumentException("Ingresa «n objetivo».");
        int n = Integer.parseInt(p[0]), target = Integer.parseInt(p[1]);
        if (n < 1 || n > 100_000_000) throw new IllegalArgumentException("n debe estar entre 1 y 100000000.");
        long lin = 0; boolean foundL = false;
        for (int i = 1; i <= n; i++) { lin++; if (i == target) { foundL = true; break; } }
        long bin = 0; int lo = 1, hi = n; boolean foundB = false;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2; bin++;
            if (mid == target) { foundB = true; break; }
            if (mid < target) lo = mid + 1; else hi = mid - 1;
        }
        return "Arreglo ordenado 1.." + n + ", objetivo = " + target + (foundL ? "" : " (no está en el arreglo)") + "\n\n"
                + "Búsqueda lineal  O(n)     : " + lin + " comparaciones\n"
                + "Búsqueda binaria O(log n) : " + bin + " comparaciones\n\n"
                + (foundB == foundL ? "Ambas dan el mismo resultado, pero la binaria trabaja mucho menos." : "");
    }

    static String growthTable(String input) {
        StringBuilder sb = new StringBuilder(String.format("%-6s %-8s %-10s %-12s %-14s %-22s%n",
                "n", "O(log n)", "O(n)", "O(n log n)", "O(n^2)", "O(2^n)"));
        for (String t : input.trim().split("[\\s,]+")) {
            int n = Integer.parseInt(t);
            if (n < 1 || n > 1000) throw new IllegalArgumentException("Usa valores de n entre 1 y 1000.");
            long log = Math.round(Math.log(n) / Math.log(2));
            String exp = n <= 62 ? String.valueOf(1L << n) : "> 10^18";
            sb.append(String.format("%-6d %-8d %-10d %-12d %-14d %-22s%n", n, log, n, n * log, (long) n * n, exp));
        }
        return sb.toString();
    }

    static String subsetSum(String input) {
        String[] lines = input.trim().split("\\R");
        if (lines.length < 2) throw new IllegalArgumentException("Línea 1: números, línea 2: objetivo.");
        String[] ns = lines[0].trim().split("\\s+");
        int n = ns.length;
        if (n > 22) throw new IllegalArgumentException("Usa a lo más 22 números.");
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = Integer.parseInt(ns[i]);
        int target = Integer.parseInt(lines[1].trim());

        long checked = 0; int found = -1;
        for (int mask = 0; mask < (1 << n); mask++) {
            checked++;
            long sum = 0;
            for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) sum += a[i];
            if (sum == target) { found = mask; break; }
        }
        StringBuilder sb = new StringBuilder("Conjunto: " + Arrays.toString(a) + ", objetivo: " + target + "\n\n");
        sb.append("1) BÚSQUEDA (fuerza bruta, hasta 2^n = ").append(1L << n).append(" subconjuntos)\n");
        if (found < 0) return sb.append("   Se evaluaron ").append(checked).append(" subconjuntos: no hay solución.").toString();
        List<Integer> cert = new ArrayList<>();
        for (int i = 0; i < n; i++) if ((found >> i & 1) == 1) cert.add(a[i]);
        sb.append("   Se evaluaron ").append(checked).append(" subconjuntos. Solución: ").append(cert).append("\n\n");
        int check = 0; for (int x : cert) check += x;
        sb.append("2) VERIFICACIÓN del certificado (tiempo polinomial, O(n))\n");
        sb.append("   ").append(cert).append(" suma ").append(check).append(check == target ? " = " : " != ").append(target)
          .append("  ->  ").append(check == target ? "válido" : "inválido");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    //  Interfaz
    // ------------------------------------------------------------------
    private JPanel createCover() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(NAVY);
        top.setBorder(new EmptyBorder(18, 25, 18, 25));
        JLabel system = new JLabel("SISTEMA DE INFORMACIÓN ACADÉMICO");
        system.setForeground(Color.WHITE);
        system.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(system, BorderLayout.WEST);
        page.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(BG);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(35, 55, 35, 55));

        JLabel eyebrow = centered("ESCUELA POLITÉCNICA NACIONAL", 18, Font.BOLD, TEAL);
        JLabel title = centered("Estructura de Datos", 38, Font.BOLD, NAVY);
        JLabel title2 = centered("y Algoritmos II", 38, Font.BOLD, NAVY);
        JLabel subtitle = centered("Aprende, explora y ejecuta ejemplos prácticos", 16, Font.PLAIN, new Color(100, 113, 126));

        JPanel logos = new JPanel(new GridLayout(1, 2, 22, 0));
        logos.setOpaque(false);
        logos.setMaximumSize(new Dimension(460, 115));
        logos.setPreferredSize(new Dimension(460, 115));
        logos.add(createLogoPlaceholder("LOGOTIPO 1", "Coloca aquí el logo de la EPN"));
        logos.add(createLogoPlaceholder("LOGOTIPO 2", "Coloca aquí el segundo logo"));

        JButton enter = primaryButton("INGRESAR");
        enter.setAlignmentX(Component.CENTER_ALIGNMENT);
        enter.setPreferredSize(new Dimension(210, 48));
        enter.setMaximumSize(new Dimension(210, 48));
        enter.addActionListener(e -> cards.show(root, "index"));

        center.add(Box.createVerticalGlue());
        center.add(eyebrow);
        center.add(Box.createVerticalStrut(16));
        center.add(title);
        center.add(title2);
        center.add(Box.createVerticalStrut(12));
        center.add(subtitle);
        center.add(Box.createVerticalStrut(32));
        center.add(logos);
        center.add(Box.createVerticalStrut(30));
        center.add(enter);
        center.add(Box.createVerticalGlue());
        page.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Proyecto académico | Java Swing", SwingConstants.CENTER);
        footer.setForeground(new Color(120, 130, 140));
        footer.setBorder(new EmptyBorder(0, 0, 18, 0));
        page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JLabel centered(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setForeground(color);
        l.setFont(new Font("SansSerif", style, size));
        return l;
    }

    private JPanel createLogoPlaceholder(String heading, String hint) {
        JPanel box = new JPanel();
        box.setBackground(Color.WHITE);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new CompoundBorder(new LineBorder(new Color(205, 216, 225), 1, true), new EmptyBorder(15, 12, 15, 12)));
        JLabel icon = centered("＋", 27, Font.BOLD, TEAL);
        JLabel name = centered(heading, 13, Font.BOLD, NAVY);
        JLabel small = centered(hint, 10, Font.PLAIN, new Color(115, 125, 135));
        box.add(Box.createVerticalGlue());
        box.add(icon);
        box.add(name);
        box.add(Box.createVerticalStrut(4));
        box.add(small);
        box.add(Box.createVerticalGlue());
        return box;
    }

    private JPanel createIndex() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(28, 38, 28, 38));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Índice de contenidos");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        title.setForeground(NAVY);
        JLabel desc = new JLabel("Selecciona un tema para explorar su contenido y sus ejemplos.");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 14));
        desc.setForeground(new Color(105, 117, 128));
        titles.add(title);
        titles.add(Box.createVerticalStrut(6));
        titles.add(desc);
        header.add(titles, BorderLayout.WEST);

        JButton home = secondaryButton("← Portada");
        home.addActionListener(e -> cards.show(root, "cover"));
        header.add(home, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(4, 1, 0, 13));
        list.setOpaque(false);
        int number = 1;
        for (String name : topics.keySet()) list.add(createTopicButton(number++, name, topics.get(name)));
        page.add(list, BorderLayout.CENTER);

        JLabel footer = new JLabel("Estructura de Datos y Algoritmos II  •  Escuela Politécnica Nacional");
        footer.setForeground(new Color(120, 130, 140));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        page.add(footer, BorderLayout.SOUTH);
        return page;
    }

    private JPanel createTopicButton(int number, String name, Topic topic) {
        JPanel card = new JPanel(new BorderLayout(18, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(218, 226, 233), 1, true), new EmptyBorder(14, 18, 14, 18)));

        JLabel num = new JLabel(String.format("%02d", number), SwingConstants.CENTER);
        num.setOpaque(true);
        num.setBackground(new Color(226, 239, 237));
        num.setForeground(TEAL);
        num.setFont(new Font("SansSerif", Font.BOLD, 23));
        num.setPreferredSize(new Dimension(62, 62));
        card.add(num, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(name);
        heading.setFont(new Font("SansSerif", Font.BOLD, 18));
        heading.setForeground(NAVY);
        JLabel description = new JLabel(topic.description);
        description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setForeground(new Color(105, 117, 128));
        text.add(Box.createVerticalGlue());
        text.add(heading);
        text.add(Box.createVerticalStrut(6));
        text.add(description);
        text.add(Box.createVerticalGlue());
        card.add(text, BorderLayout.CENTER);

        JButton open = primaryButton("Abrir tema  →");
        open.addActionListener(e -> showTopic(topic));
        card.add(open, BorderLayout.EAST);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showTopic(topic); }
        });
        return card;
    }

    private void showTopic(Topic topic) {
        JPanel detail = new JPanel(new BorderLayout(0, 10));
        detail.setBackground(BG);
        detail.setBorder(new EmptyBorder(24, 42, 24, 42));

        JButton back = secondaryButton("← Volver al índice");
        back.addActionListener(e -> cards.show(root, "index"));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(back, BorderLayout.WEST);
        detail.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(left(label(topic.title, 30, Font.BOLD, NAVY)));
        content.add(Box.createVerticalStrut(10));
        content.add(left(label("<html><div style='width:650px'>" + topic.description + "</div></html>", 15, Font.PLAIN, TEXT)));
        content.add(Box.createVerticalStrut(16));
        content.add(left(label(topic.title.equals("Grafos") ? "Subtemas" : "Contenidos del tema", 18, Font.BOLD, TEAL)));
        content.add(Box.createVerticalStrut(8));
        for (String item : topic.items) {
            content.add(left(label("  •  " + item, 15, Font.PLAIN, TEXT)));
            content.add(Box.createVerticalStrut(5));
        }
        content.add(Box.createVerticalStrut(14));
        content.add(left(createExamplePanel(topic)));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        detail.add(scroll, BorderLayout.CENTER);

        if (currentDetail != null) root.remove(currentDetail);
        currentDetail = detail;
        root.add(detail, "detail");
        cards.show(root, "detail");
    }

    private JPanel createExamplePanel(Topic topic) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBackground(Color.WHITE);
        box.setBorder(new CompoundBorder(new LineBorder(new Color(205, 216, 225), 1, true), new EmptyBorder(16, 18, 16, 18)));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));

        JComboBox<Example> combo = new JComboBox<>(topic.examples);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JLabel title = label("Ejemplo ejecutable:", 15, Font.BOLD, TEAL);
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(combo, BorderLayout.CENTER);

        JLabel help = label("", 12, Font.PLAIN, new Color(105, 117, 128));
        JTextArea in = new JTextArea(6, 30);
        in.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        in.setBorder(new EmptyBorder(4, 6, 4, 6));
        JTextArea out = new JTextArea(10, 30);
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        out.setEditable(false);
        out.setBackground(new Color(247, 249, 251));
        out.setBorder(new EmptyBorder(4, 6, 4, 6));

        Runnable load = () -> {
            Example ex = (Example) combo.getSelectedItem();
            help.setText("<html><div style='width:650px'>" + ex.help + "</div></html>");
            in.setText(ex.defaultInput);
            out.setText("");
        };
        combo.addActionListener(e -> load.run());
        load.run();

        JButton run = primaryButton("▶ Ejecutar");
        run.addActionListener(e -> {
            Example ex = (Example) combo.getSelectedItem();
            try {
                out.setForeground(TEXT);
                out.setText(ex.runner.apply(in.getText()));
            } catch (RuntimeException ex2) {
                out.setForeground(new Color(170, 40, 40));
                out.setText("Error: " + ex2.getMessage());
            }
            out.setCaretPosition(0);
        });
        JButton reset = secondaryButton("Restablecer");
        reset.addActionListener(e -> load.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(run);
        buttons.add(reset);

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        JPanel inTop = new JPanel(new BorderLayout(0, 4));
        inTop.setOpaque(false);
        inTop.add(header, BorderLayout.NORTH);
        inTop.add(help, BorderLayout.CENTER);
        mid.add(inTop, BorderLayout.NORTH);
        JScrollPane inScroll = new JScrollPane(in);
        inScroll.setBorder(new TitledBorder("Datos de entrada"));
        JScrollPane outScroll = new JScrollPane(out);
        outScroll.setBorder(new TitledBorder("Resultado"));
        JPanel areas = new JPanel(new GridLayout(1, 2, 12, 0));
        areas.setOpaque(false);
        areas.add(inScroll);
        areas.add(outScroll);
        mid.add(areas, BorderLayout.CENTER);

        box.add(mid, BorderLayout.CENTER);
        box.add(buttons, BorderLayout.SOUTH);
        return box;
    }

    private JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(TEAL);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBorder(new EmptyBorder(12, 20, 12, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(NAVY);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(new CompoundBorder(new LineBorder(new Color(205, 216, 225), 1, true), new EmptyBorder(9, 13, 9, 13)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static class Example {
        final String name, help, defaultInput;
        final Function<String, String> runner;
        Example(String name, String help, String defaultInput, Function<String, String> runner) {
            this.name = name; this.help = help; this.defaultInput = defaultInput; this.runner = runner;
        }
        @Override public String toString() { return name; }
    }

    private static class Topic {
        final String title, description;
        final String[] items;
        final Example[] examples;
        Topic(String title, String description, String[] items, Example[] examples) {
            this.title = title; this.description = description; this.items = items; this.examples = examples;
        }
    }
}
