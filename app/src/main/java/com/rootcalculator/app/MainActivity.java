
package com.rootcalculator.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView display;
    private String expression = "";

    private final String[][] keys = {
        {"AC", "DEL", "(", ")", "÷"},
        {"sin", "cos", "tan", "√", "×"},
        {"7", "8", "9", "π", "−"},
        {"4", "5", "6", "e", "+"},
        {"1", "2", "3", ".", "%"},
        {"0", "ln", "log", "^", "="}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 24, 16, 16);
        root.setBackgroundColor(Color.rgb(14, 18, 28));

        TextView title = new TextView(this);
        title.setText("ROOT CALCULATOR");
        title.setTextSize(18);
        title.setTextColor(Color.rgb(100, 230, 170));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
            -1, 52
        ));

        display = new TextView(this);
        display.setText("0");
        display.setTextSize(32);
        display.setTextColor(Color.WHITE);
        display.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        display.setPadding(12, 12, 12, 12);
        display.setSingleLine(false);

        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
            -1, 0, 1
        );
        dp.setMargins(0, 8, 0, 16);
        root.addView(display, dp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        grid.setRowCount(6);

        for (String[] row : keys) {
            for (String key : row) {
                Button b = new Button(this);
                b.setText(key);
                b.setTextSize(16);
                b.setAllCaps(false);
                b.setTextColor(Color.WHITE);
                b.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        key.equals("=")
                            ? Color.rgb(35, 190, 125)
                            : isOperator(key)
                                ? Color.rgb(43, 58, 79)
                                : Color.rgb(29, 36, 51)
                    )
                );

                GridLayout.LayoutParams p =
                    new GridLayout.LayoutParams();
                p.width = 0;
                p.height = 0;
                p.columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                );
                p.rowSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1f
                );
                p.setMargins(4, 4, 4, 4);
                grid.addView(b, p);

                b.setOnClickListener(v -> press(key));
            }
        }

        root.addView(grid, new LinearLayout.LayoutParams(
            -1, 0, 3
        ));

        setContentView(root);
    }

    private boolean isOperator(String s) {
        return s.equals("÷") || s.equals("×")
            || s.equals("+") || s.equals("−")
            || s.equals("=") || s.equals("AC")
            || s.equals("DEL");
    }

    private void press(String key) {
        if (key.equals("AC")) {
            expression = "";
            display.setText("0");
            return;
        }

        if (key.equals("DEL")) {
            if (!expression.isEmpty()) {
                expression = expression.substring(
                    0, expression.length() - 1
                );
            }
            display.setText(expression.isEmpty() ? "0" : expression);
            return;
        }

        if (key.equals("=")) {
            try {
                double result = new Parser(expression).parse();
                if (!Double.isFinite(result)) {
                    throw new ArithmeticException();
                }
                expression = String.format(
                    Locale.US, "%.10f", result
                ).replaceAll("0+$", "").replaceAll("\\.$", "");
                display.setText(expression);
            } catch (Exception e) {
                display.setText("Invalid expression");
                Toast.makeText(
                    this, "হিসাবটি সঠিক নয়", Toast.LENGTH_SHORT
                ).show();
                expression = "";
            }
            return;
        }

        if (key.equals("sin") || key.equals("cos")
                || key.equals("tan") || key.equals("√")
                || key.equals("ln") || key.equals("log")) {
            expression += key + "(";
        } else if (key.equals("π")) {
            expression += "pi";
        } else {
            expression += key;
        }

        display.setText(expression);
    }

    private static class Parser {
        private final String s;
        private int pos = -1;
        private int ch;

        Parser(String input) {
            s = input.replace("×", "*")
                     .replace("÷", "/")
                     .replace("−", "-")
                     .replace("pi", "PI");
        }

        void next() {
            ch = (++pos < s.length()) ? s.charAt(pos) : -1;
        }

        boolean eat(int c) {
            while (ch == ' ') next();
            if (ch == c) {
                next();
                return true;
            }
            return false;
        }

        double parse() {
            next();
            double x = expr();
            if (pos < s.length()) throw new RuntimeException();
            return x;
        }

        double expr() {
            double x = term();
            for (;;) {
                if (eat('+')) x += term();
                else if (eat('-')) x -= term();
                else return x;
            }
        }

        double term() {
            double x = power();
            for (;;) {
                if (eat('*')) x *= power();
                else if (eat('/')) x /= power();
                else if (eat('%')) x %= 100;
                else return x;
            }
        }

        double power() {
            double x = unary();
            if (eat('^')) x = Math.pow(x, power());
            return x;
        }

        double unary() {
            if (eat('+')) return unary();
            if (eat('-')) return -unary();

            double x;
            int start = pos;

            if (eat('(')) {
                x = expr();
                if (!eat(')')) throw new RuntimeException();
            } else if (ch >= '0' && ch <= '9' || ch == '.') {
                while (ch >= '0' && ch <= '9' || ch == '.') next();
                x = Double.parseDouble(s.substring(start, pos));
            } else if (Character.isLetter(ch)) {
                while (Character.isLetter(ch)) next();
                String name = s.substring(start, pos);

                if (name.equals("PI")) return Math.PI;
                if (name.equals("e")) return Math.E;

                if (!eat('(')) throw new RuntimeException();
                x = expr();
                if (!eat(')')) throw new RuntimeException();

                switch (name) {
                    case "sin": return Math.sin(Math.toRadians(x));
                    case "cos": return Math.cos(Math.toRadians(x));
                    case "tan": return Math.tan(Math.toRadians(x));
                    case "√": return Math.sqrt(x);
                    case "ln": return Math.log(x);
                    case "log": return Math.log10(x);
                    default: throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }

            return x;
        }
    }
}
