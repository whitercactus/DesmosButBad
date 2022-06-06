import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.event.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Desmos extends JFrame implements ActionListener {
    private JTextField tfield;
    private static final String S = "0.000000000000000";

    private String equation;
    private double temp, temp1, result, a;
    private static double m1, m2;
    private int k = 1, x = 0, y = 0, z = 0;
    private char ch;
    private Container cont;
    private JPanel textPanel, buttonpanel;
    private final JLabel label = new JLabel(S, JLabel.CENTER);

    private final XYSeries series = new XYSeries("Result");
    private final XYDataset dataset = new XYSeriesCollection(series);

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)` | number
            //        | functionName `(` expression `)` | functionName factor
            //        | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    if (!eat(')')) throw new RuntimeException("Missing ')'");
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        if (!eat(')')) throw new RuntimeException("Missing ')' after argument to " + func);
                    } else {
                        x = parseFactor();
                    }
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else throw new RuntimeException("Unknown function: " + func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    private Desmos() {
        this.equation = "";
        cont = getContentPane();
        cont.setLayout(new BorderLayout());
        JPanel textpanel = new JPanel();
        tfield = new JTextField(25);
        tfield.setHorizontalAlignment(SwingConstants.RIGHT);
        tfield.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent keyevent) {
                char c = keyevent.getKeyChar();
                if (c >= '0' && c <= '9') {
                } else {
                    keyevent.consume();
                }
            }
        });
        textpanel.add(tfield);
        buttonpanel = new JPanel();
        buttonpanel.setLayout(new GridLayout(8, 4, 2, 2));
        boolean t = true;

        cont.add("Center", buttonpanel);
        cont.add("North", textpanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        equation += s;
    }

    private void create() {
        JFrame f = new JFrame("√2");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Newton's Method", "X", "Y", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(1.4, 1.51);
        plot.getDomainAxis().setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        XYLineAndShapeRenderer renderer
                = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesShapesVisible(0, true);
        f.add(new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        }, BorderLayout.CENTER);
        f.add(label, BorderLayout.SOUTH);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    private class TwoWorker extends SwingWorker<Double, Double> {

        private static final int N = 5;
        private final DecimalFormat df = new DecimalFormat(S);
        double x = 1;
        private int n;

        @Override
        protected Double doInBackground() throws Exception {
            for (int i = 1; i <= N; i++) {
                x = x - (((x * x - 2) / (2 * x)));
                setProgress(i * (100 / N));
                publish(x);
                Thread.sleep(1000); // simulate latency
            }
            return x;
        }

        @Override
        protected void process(List<Double> chunks) {
            for (double d : chunks) {
                label.setText(df.format(d));
                series.add(++n, d);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(eval("((4 - 2^3 + 1) * -sqrt(3*3+4*4)) / 2"));

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        Desmos f = new Desmos();
        f.setTitle("Bad Desmos");
        f.pack();
        f.setVisible(true);
    }
}