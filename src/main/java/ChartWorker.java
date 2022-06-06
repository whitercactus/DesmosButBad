import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.*;

import net.objecthunter.exp4j.ExpressionBuilder;
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

import net.objecthunter.exp4j.Expression;

public final class ChartWorker {
    private String equation;
    private static final String S = "0.000000000000000";
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel label = new JLabel(S, JLabel.CENTER);
    private final XYSeries series = new XYSeries("Result");
    private final XYDataset dataset = new XYSeriesCollection(series);
    private JTextField tfield, func;
    private JButton solve;
    private JLabel answer;

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

    private void create() {
        JFrame f = new JFrame("Bad Desmos");
        tfield = new JTextField(32);
        solve = new JButton("Enter");
        answer = new JLabel("Answer");

        solve.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("test");
                Expression expression = new ExpressionBuilder(tfield.getText()).build();
                equation = "" + expression.evaluate();
                answer.setText(equation);
            }
        });
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(progressBar, BorderLayout.NORTH);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Bad Desmos", "X", "Y", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(-2, 2);
        plot.getDomainAxis().setStandardTickUnits(
                NumberAxis.createIntegerTickUnits());
        XYLineAndShapeRenderer renderer
                = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesShapesVisible(0, true);
        f.add(new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 600);
            }
        }, BorderLayout.CENTER);
        f.add(solve,  BorderLayout.LINE_END);
        f.add(label, BorderLayout.SOUTH);
        f.add(answer, BorderLayout.NORTH);
        f.add(tfield, BorderLayout.SOUTH);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        runCalc();
    }

    private void runCalc() {
        progressBar.setIndeterminate(true);
        TwoWorker task = new TwoWorker();
        task.addPropertyChangeListener((PropertyChangeEvent e) -> {
            if ("progress".equals(e.getPropertyName())) {
                progressBar.setIndeterminate(false);
                progressBar.setValue((Integer) e.getNewValue());
            }
        });
        task.execute();
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
        EventQueue.invokeLater(new ChartWorker()::create);
    }
}