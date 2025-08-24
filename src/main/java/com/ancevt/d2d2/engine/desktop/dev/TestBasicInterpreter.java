package com.ancevt.d2d2.engine.desktop.dev;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.ancevt.d2d2.ApplicationContext;
import com.ancevt.d2d2.D2D2;
import com.ancevt.d2d2.debug.FpsMeter;
import com.ancevt.d2d2.input.KeyCode;
import com.ancevt.d2d2.lifecycle.Application;
import com.ancevt.d2d2.scene.Bitmap;

public class TestBasicInterpreter implements Application {

    public static void main(String[] args) {
        D2D2.init(new TestBasicInterpreter());
    }

    // Dimensions for text screen (approx 40x24 text characters)
    private final int TEXT_COLS = 40;
    private final int TEXT_ROWS = 24;
    private final int CHAR_WIDTH = 6;
    private final int CHAR_HEIGHT = 6;
    private Bitmap bitmap;

    // Text buffer to hold characters on screen
    private char[][] textBuffer;
    // Cursor position (in text cell units)
    private int cursorX = 0;
    private int cursorY = 0;
    // Current input line being typed
    private StringBuilder inputLine = new StringBuilder();
    // Program storage (line number -> code)
    private TreeMap<Integer, String> program = new TreeMap<>();
    // Variables storage (variable name -> value)
    private Map<String, Integer> variables = new HashMap<>();
    // Shift key state for input
    private boolean shiftPressed = false;

    // Font patterns for characters (5x5 or 5x6 patterns mapped to a 6x6 cell)
    private static final Map<Character, int[][]> fontMap = new HashMap<>();
    // Define a color for text (white) with full opacity in the format the engine expects
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int TEXT_COLOR_INT = (TEXT_COLOR << 8) | 0xFF;

    @Override
    public void start(ApplicationContext applicationContext) {
        // Create a bitmap canvas for the text, with base resolution and scaling
        final int width = 300;   // base pixel width
        final int height = 150;  // base pixel height
        bitmap = Bitmap.create(width, height);
        bitmap.setScale(3f);  // enlarge the bitmap for visibility (3x)

        // Initialize the text buffer with spaces
        textBuffer = new char[TEXT_ROWS][TEXT_COLS];
        for (int r = 0; r < TEXT_ROWS; r++) {
            Arrays.fill(textBuffer[r], ' ');
        }

        // Print initial READY prompt
        printLine("READY");
        newLine();

        // Set up the stage for rendering and input
        applicationContext.stage().addChild(bitmap);
        applicationContext.stage().addChild(new FpsMeter());  // shows FPS in a corner

        // Keyboard input handlers
        applicationContext.stage().onKeyDown(e -> onKeyPressed(e.getKeyCode()));
        applicationContext.stage().onKeyUp(e -> onKeyReleased(e.getKeyCode()));

        // onTick will continuously render the text buffer (and blinking cursor)
        applicationContext.stage().onTick(e -> {
            bitmap.clear();
            drawTextBuffer();
            drawCursor();  // draw blinking cursor at current position
        });
    }

    private void onKeyPressed(int keyCode) {
        // Shift
        if (KeyCode.isShift(keyCode)) {
            shiftPressed = true;
            return;
        }

        // Enter (оба варианта)
        if (keyCode == KeyCode.ENTER || keyCode == KeyCode.RIGHT_ENTER) {
            executeInputLine();
            return;
        }

        // Backspace
        if (keyCode == KeyCode.BACKSPACE) {
            if (inputLine.length() > 0) {
                inputLine.deleteCharAt(inputLine.length() - 1);
                if (cursorX > 0) {
                    cursorX--;
                } else if (cursorY > 0) {
                    cursorY--;
                    cursorX = TEXT_COLS - 1;
                }
                textBuffer[cursorY][cursorX] = ' ';
            }
            return;
        }

        // Пробел (для надёжности — но ниже мы всё равно ловим ASCII 32)
        if (keyCode == KeyCode.SPACE) {
            if (cursorX < TEXT_COLS) {
                inputLine.append(' ');
                textBuffer[cursorY][cursorX++] = ' ';
            }
            return;
        }

        // Любой печатный символ → в буфер
        char ch = mapKeyCodeToChar(keyCode, shiftPressed);
        if (ch != 0) {
            if (cursorX >= TEXT_COLS) return;
            inputLine.append(ch);
            textBuffer[cursorY][cursorX++] = ch;
        }
    }

    private void onKeyReleased(int keyCode) {
        if (KeyCode.isShift(keyCode)) {
            shiftPressed = false;
        }
    }

    private char mapKeyCodeToChar(int keyCode, boolean shift) {
        // Буквы A..Z (твои KeyCode совпадают с ASCII 65..90)
        if (keyCode >= KeyCode.A && keyCode <= KeyCode.Z) {
            return (char) keyCode; // уже верхний регистр
        }

        // Цифры верхнего ряда 0..9
        if (keyCode >= KeyCode.N_0 && keyCode <= KeyCode.N_9) {
            return (char) keyCode; // ASCII 48..57
        }

        // Цифры numpad 0..9
        if (keyCode >= KeyCode.NUM_0 && keyCode <= KeyCode.NUM_9) {
            int digit = keyCode - KeyCode.NUM_0; // 0..9
            return (char) ('0' + digit);
        }

        // Пробел и прочие печатные ASCII (пунктуация, кавычки, скобки и т.д.)
        // Важно: это сработает, только если твой input-слой реально отдаёт ASCII-код символа
        if (keyCode >= 32 && keyCode <= 126) {
            return (char) keyCode;
        }

        // Не печатный символ
        return (char) 0;
    }


    /** Execute the current input line (entered when Enter is pressed). */
    private void executeInputLine() {
        String line = inputLine.toString().trim();  // get the input text
        inputLine.setLength(0);  // clear the input buffer for the next line
        // Move cursor to next line for output or next input
        newLine();

        if (line.isEmpty()) {
            // If empty line, just ignore (or we could move on)
            return;
        }
        // Check if the line starts with a number (program line entry)
        int firstSpace = line.indexOf(' ');
        String firstToken = (firstSpace >= 0 ? line.substring(0, firstSpace) : line);
        boolean isNumberedLine = false;
        int lineNumber = -1;
        try {
            lineNumber = Integer.parseInt(firstToken);
            isNumberedLine = true;
        } catch (NumberFormatException e) {
            isNumberedLine = false;
        }

        if (isNumberedLine) {
            // Program line input
            String code = "";
            if (firstSpace >= 0) {
                code = line.substring(firstSpace + 1).trim();
            }
            if (code.equals("") && lineNumber > 0) {
                // If no code after line number, delete that program line
                program.remove(lineNumber);
            } else if (lineNumber > 0) {
                // Store/replace the program line
                program.put(lineNumber, code);
            }
            // After adding a line, don't execute anything immediately (just store it). Await next input.
        } else {
            // Immediate command or expression
            interpretCommand(line);
        }
    }

    /** Interpret and execute an immediate command or expression (for non-numbered input lines). */
    private void interpretCommand(String input) {
        String cmd = input.toUpperCase();
        // BASIC keywords handling
        if (cmd.equals("RUN")) {
            runProgram();
        } else if (cmd.equals("LIST")) {
            listProgram();
        } else if (cmd.equals("NEW")) {
            // Clear program and variables
            program.clear();
            variables.clear();
            printLine("READY");  // indicate ready after clearing
            newLine();
        } else if (cmd.startsWith("PRINT")) {
            // Handle PRINT command, e.g. "PRINT 5+3" or "PRINT X" or "PRINT \"TEXT\""
            String toPrint = cmd.substring(5).trim();  // text after "PRINT"
            if (toPrint.startsWith("\"") && toPrint.endsWith("\"")) {
                // Quoted string literal
                String literal = toPrint.substring(1, toPrint.length() - 1);
                printLine(literal);
            } else if (!toPrint.isEmpty()) {
                // Evaluate expression
                Integer result = evaluateExpression(toPrint);
                if (result != null) {
                    printLine(result.toString());
                } else {
                    printLine("ERROR");
                }
            } else {
                // "PRINT" with nothing else - just a blank line
                newLine();
            }
            newLine();
        } else if (cmd.startsWith("?")) {
            // '?' as shorthand for PRINT (Atari BASIC allowed this)
            String expr = cmd.substring(1).trim();
            if (expr.startsWith("\"") && expr.endsWith("\"")) {
                printLine(expr.substring(1, expr.length() - 1));
            } else if (!expr.isEmpty()) {
                Integer result = evaluateExpression(expr);
                if (result != null) {
                    printLine(result.toString());
                } else {
                    printLine("ERROR");
                }
            }
            newLine();
        } else if (cmd.startsWith("LET ")) {
            // LET X = ... (optional LET keyword)
            String assignment = cmd.substring(4).trim();
            executeAssignment(assignment);
        } else if (cmd.contains("=") && !cmd.startsWith("REM")) {
            // If the command contains '=' and is not a REMark, treat it as an assignment X=...
            executeAssignment(cmd);
        } else {
            // Unknown command or just an expression on its own
            // We can attempt to evaluate a bare expression (like just typing "2+2")
            Integer result = evaluateExpression(cmd);
            if (result != null) {
                printLine(result.toString());
                newLine();
            } else {
                printLine("ERROR");
                newLine();
            }
        }
    }

    /** Execute a stored BASIC program from beginning (RUN command). */
    private void runProgram() {
        // Start from the first line in the program
        if (program.isEmpty()) {
            return;  // nothing to run
        }
        // Iterator through lines, but we need to be able to jump for GOTO.
        Integer currentLineNumber = program.firstKey();
        executionLoop:
        while (currentLineNumber != null) {
            String code = program.get(currentLineNumber);
            if (code == null) {
                // If a line number has no code (shouldn't happen since we remove empty ones), skip
                currentLineNumber = program.higherKey(currentLineNumber);
                continue;
            }
            String lineCode = code.trim();
            String lineCodeUpper = lineCode.toUpperCase();
            // Process the line code
            if (lineCodeUpper.startsWith("REM")) {
                // Comment line, do nothing
                // (Atari BASIC uses REM for comments)
            } else if (lineCodeUpper.startsWith("PRINT")) {
                String arg = lineCode.substring(5).trim();
                if (arg.startsWith("\"") && arg.endsWith("\"")) {
                    printLine(arg.substring(1, arg.length() - 1));
                } else if (!arg.isEmpty()) {
                    Integer result = evaluateExpression(arg);
                    if (result != null) {
                        printLine(result.toString());
                    } else {
                        printLine("ERROR");
                    }
                } else {
                    // plain PRINT with no argument
                    newLine();
                }
                newLine();
            } else if (lineCodeUpper.startsWith("?")) {
                // Allow '?' in program as shorthand for PRINT
                String arg = lineCode.substring(1).trim();
                if (arg.startsWith("\"") && arg.endsWith("\"")) {
                    printLine(arg.substring(1, arg.length() - 1));
                } else if (!arg.isEmpty()) {
                    Integer result = evaluateExpression(arg);
                    if (result != null) {
                        printLine(result.toString());
                    } else {
                        printLine("ERROR");
                    }
                }
                newLine();
            } else if (lineCodeUpper.startsWith("GOTO")) {
                // Jump to a line number
                String targetStr = lineCode.substring(4).trim();
                try {
                    int target = Integer.parseInt(targetStr);
                    // Find the smallest key >= target (or exactly target)
                    if (program.containsKey(target)) {
                        currentLineNumber = target;
                        continue;  // jump to target line directly
                    } else {
                        // If line not found, we can break with an error
                        printLine("ERROR: LINE " + target + " MISSING");
                        newLine();
                        break executionLoop;
                    }
                } catch (NumberFormatException e) {
                    // Invalid GOTO argument
                    printLine("ERROR: BAD GOTO");
                    newLine();
                    break executionLoop;
                }
            } else if (lineCodeUpper.startsWith("LET ")) {
                // Assignment with optional LET
                String assignment = lineCode.substring(4).trim();
                executeAssignment(assignment);
            } else if (lineCodeUpper.contains("=") && !lineCodeUpper.startsWith("IF")) {
                // Treat it as assignment (assuming no IF statements for simplicity)
                executeAssignment(lineCode);
            } else if (lineCodeUpper.startsWith("END") || lineCodeUpper.startsWith("STOP")) {
                // End the program execution
                break executionLoop;
            } else if (lineCodeUpper.startsWith("IF")) {
                // (IF/THEN not implemented in this simple interpreter)
                printLine("ERROR: IF NOT SUPPORTED");
                newLine();
            } else {
                // Unrecognized line content
                // We attempt to evaluate it as an expression (maybe a standalone expression)
                Integer result = evaluateExpression(lineCode);
                if (result != null) {
                    printLine(result.toString());
                    newLine();
                }
            }
            // Move to next line in numeric order
            currentLineNumber = program.higherKey(currentLineNumber);
        }
        // After running program, show READY prompt (like real BASIC does)
        printLine("READY");
        newLine();
    }

    /** List all program lines to the screen (for LIST command). */
    private void listProgram() {
        for (Map.Entry<Integer, String> entry : program.entrySet()) {
            Integer num = entry.getKey();
            String code = entry.getValue();
            String listing = num + " " + code;
            printLine(listing);
            newLine();
        }
        // After listing, just leave cursor at next line for input
    }

    /** Parse and execute an assignment statement (e.g., X=5 or TOTAL=TOTAL+1). */
    private void executeAssignment(String stmt) {
        // Split at the first '='
        int eqIndex = stmt.indexOf('=');
        if (eqIndex <= 0) {
            return; // not a valid assignment
        }
        String varName = stmt.substring(0, eqIndex).trim();
        String expr = stmt.substring(eqIndex + 1).trim();
        // In BASIC, variable names can be multiple letters, but let's treat the whole varName as one (case-insensitive)
        varName = varName.toUpperCase();
        // Evaluate the expression on right side
        Integer value = evaluateExpression(expr);
        if (value != null) {
            variables.put(varName, value);
        } else {
            printLine("ERROR");
            newLine();
        }
    }

    /** Evaluate a numeric expression (with +, -, *, and integer values and variables). */
    private Integer evaluateExpression(String expr) {
        try {
            // Simple expression parser:
            // We will handle +, -, * and basic precedence (* before +/-, left-associative).
            // No parentheses or division for simplicity.
            // We also handle variables (A, X, etc.) by substituting their values.
            String tokens = expr.trim();
            if (tokens.equals("")) return 0;
            // Replace variable names with their values
            // We assume variables are one or more letters. Find continuous letter sequences and replace with value.
            // (This is a simple approach and could mis-replace parts of words if not careful, but in BASIC context it's fine.)
            // For safety, we replace longest names first to handle multi-letter vars.
            // Here, we'll just replace all occurrences of known variables.
            for (String var : variables.keySet()) {
                if (var.length() > 0) {
                    // Replace case-insensitively
                    // Regex word boundary might help to not replace within other tokens accidentally
                    tokens = tokens.replaceAll("(?i)\\b" + var + "\\b", variables.get(var).toString());
                }
            }
            // Now tokens should contain only numbers and + - * (and possibly spaces).
            // Implement a basic evaluator:
            return evaluateSimpleExpression(tokens);
        } catch (Exception e) {
            return null;  // on any error, return null to indicate failure
        }
    }

    /** Helper: evaluate an expression string containing only numbers and + - * (no variables or spaces). */
    private Integer evaluateSimpleExpression(String expr) throws Exception {
        // Remove spaces
        expr = expr.replace(" ", "");
        // Handle addition/subtraction and multiplication precedence
        // We can implement by first handling "*" then + and -.
        // Split by + and - at top level (not inside any parentheses, but we have no parentheses here).
        // Actually implement a simple left-to-right because * has higher precedence, we should do multiplication first.

        // We will do it in two passes: first handle '*' operations.
        // Split on + and - first to get terms, but we need to keep track of the operators.
        // Instead, let's implement a straightforward parsing:
        int total = 0;
        int current = 0;
        char op = '+';  // last seen addition/subtraction operator
        for (int i = 0; i < expr.length();) {
            char c = expr.charAt(i);
            if (Character.isDigit(c) || c == '-') {
                // Parse a number (the '-' might be a sign for negative number)
                int start = i;
                // If we encounter a '-' that is not at start and the previous char is an operator, it should be treated as a negative sign.
                if (c == '-') {
                    // If it's a negative sign (like "-5" or part of expression like 3 + -2), handle as part of number
                    // To differentiate unary minus vs subtraction:
                    // If at start of expression or after another operator, treat as unary minus.
                    boolean unary = (start == 0) || (expr.charAt(start-1) == '+' || expr.charAt(start-1) == '-' || expr.charAt(start-1) == '*');
                    if (unary) {
                        i++; // include '-' in the number
                        // Continue parsing the digits following the minus
                        while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                            i++;
                        }
                        // Now parse the number including the minus sign
                        int value = Integer.parseInt(expr.substring(start, i));
                        current = value;
                        continue; // go back to loop without resetting op because op is already used for this number differently in unary context
                    }
                }
                // If not a unary minus or just a normal number, parse the full integer
                while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                    i++;
                }
                int value = Integer.parseInt(expr.substring(start, i));
                current = value;
            }
            if (i < expr.length()) {
                char nextOp = expr.charAt(i);
                if (nextOp == '*' || nextOp == '+' || nextOp == '-') {
                    // If it's a multiplication, handle it immediately with the next number (to respect precedence)
                    if (nextOp == '*') {
                        // Multiply current with the next factor
                        i++;
                        // Parse the next number immediately after '*'
                        int start = i;
                        // Allow a leading minus for negative factor
                        if (i < expr.length() && expr.charAt(i) == '-') {
                            i++;
                        }
                        while (i < expr.length() && Character.isDigit(expr.charAt(i))) {
                            i++;
                        }
                        int value = Integer.parseInt(expr.substring(start, i));
                        current = current * value;
                        // Now `current` holds the result of the multiplication with the next number.
                        // We do not update `op` or `total` yet because we might chain multiple * in a row.
                        // We effectively keep `op` as the last addition/subtraction op.
                        // Continue loop without finalizing addition/subtraction.
                        continue;
                    } else {
                        // For + or -, we finalize the previous addition/subtraction operation.
                        if (op == '+') {
                            total += current;
                        } else if (op == '-') {
                            total -= current;
                        }
                        // Update the last seen op to this plus or minus
                        op = nextOp;
                    }
                }
                i++;
            } else {
                // Reached end of expression
                if (op == '+') {
                    total += current;
                } else if (op == '-') {
                    total -= current;
                }
            }
        }
        return total;
    }

    /** Print a line of text to the screen (at the current cursor position) */
    private void printLine(String text) {
        if (text == null) return;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (cursorX >= TEXT_COLS) {
                // If reaching end of line, wrap to next line
                newLine();
            }
            if (cursorY >= TEXT_ROWS) {
                scrollUp();
            }
            // Put character into buffer
            if (c == '\n') {
                // if the string contains newline, move to next line
                newLine();
            } else {
                // Only print characters that are in our font (letters, digits, basic punctuation).
                if (!fontMap.containsKey(Character.toUpperCase(c))) {
                    c = ' '; // replace unknown char with space
                }
                textBuffer[cursorY][cursorX] = c;
                cursorX++;
            }
        }
    }

    /** Move cursor to the beginning of the next line (with scrolling if needed). */
    private void newLine() {
        cursorX = 0;
        cursorY++;
        if (cursorY >= TEXT_ROWS) {
            // If we ran out of screen space, scroll up
            scrollUp();
            cursorY = TEXT_ROWS - 1;
        }
    }

    /** Scroll the text buffer up by one line (discard the top line). */
    private void scrollUp() {
        for (int r = 1; r < TEXT_ROWS; r++) {
            textBuffer[r-1] = Arrays.copyOf(textBuffer[r], TEXT_COLS);
        }
        // Clear the last line
        textBuffer[TEXT_ROWS - 1] = new char[TEXT_COLS];
        Arrays.fill(textBuffer[TEXT_ROWS - 1], ' ');
        cursorY--;  // adjust cursor one line up
    }

    /** Draw the entire text buffer onto the bitmap. */
    private void drawTextBuffer() {
        for (int r = 0; r < TEXT_ROWS; r++) {
            for (int c = 0; c < TEXT_COLS; c++) {
                char ch = textBuffer[r][c];
                if (ch != ' ') {
                    drawChar(c, r, ch);
                }
            }
        }
    }

    /** Draw a single character from our font at the given text cell coordinates. */
    private void drawChar(int col, int row, char ch) {
        int[][] pattern = fontMap.get(Character.toUpperCase(ch));
        if (pattern == null) {
            return;  // if character not defined, skip
        }
        int px = col * CHAR_WIDTH;
        int py = row * CHAR_HEIGHT;
        for (int dy = 0; dy < pattern.length; dy++) {
            for (int dx = 0; dx < pattern[dy].length; dx++) {
                if (pattern[dy][dx] == 1) {
                    bitmap.setPixel(px + dx, py + dy, TEXT_COLOR_INT);
                }
            }
        }
    }

    /** Draw a blinking cursor (underscore) at the current cursor position to indicate where typing occurs. */
    private void drawCursor() {
        // Simple blinking effect: visible 500ms, off 500ms
        long t = System.currentTimeMillis();
        if ((t / 500) % 2 == 0) {  // toggle every half-second
            if (cursorY < TEXT_ROWS && cursorX < TEXT_COLS) {
                // Draw an underscore character as cursor
                int px = cursorX * CHAR_WIDTH;
                int py = cursorY * CHAR_HEIGHT;
                // Draw a horizontal line (or use an underscore pattern)
                for (int dx = 0; dx < CHAR_WIDTH - 1; dx++) {
                    // place cursor line at bottom row of the char cell
                    bitmap.setPixel(px + dx, py + CHAR_HEIGHT - 1, TEXT_COLOR_INT);
                }
            }
        }
    }

    // Font pattern initialization (static block to fill fontMap)
    static {
        // Define patterns using strings for readability ('X' = 1, '.' = 0)
        String[] A = {
                ".XXX.",
                "X...X",
                "X...X",
                "XXXXX",
                "X...X",
                "X...X"
        };
        String[] B = {
                "XXXX.",
                "X...X",
                "XXXX.",
                "X...X",
                "X...X",
                "XXXX."
        };
        String[] C = {
                ".XXXX",
                "X....",
                "X....",
                "X....",
                "X....",
                ".XXXX"
        };
        String[] D = {
                "XXXX.",
                "X...X",
                "X...X",
                "X...X",
                "X...X",
                "XXXX."
        };
        String[] E = {
                "XXXXX",
                "X....",
                "XXX..",
                "X....",
                "X....",
                "XXXXX"
        };
        String[] F = {
                "XXXXX",
                "X....",
                "XXX..",
                "X....",
                "X....",
                "X...."
        };
        String[] G = {
                ".XXXX",
                "X....",
                "X....",
                "X..XX",
                "X...X",
                ".XXXX"
        };
        String[] H = {
                "X...X",
                "X...X",
                "XXXXX",
                "X...X",
                "X...X",
                "X...X"
        };
        String[] I = {
                "XXXXX",
                "..X..",
                "..X..",
                "..X..",
                "..X..",
                "XXXXX"
        };
        String[] J = {
                "..XXX",
                "...X.",
                "...X.",
                "...X.",
                "X..X.",
                ".XX.."
        };
        String[] K = {
                "X...X",
                "X..X.",
                "XXX..",
                "X..X.",
                "X...X",
                "X...X"
        };
        String[] L = {
                "X....",
                "X....",
                "X....",
                "X....",
                "X....",
                "XXXXX"
        };
        String[] M = {
                "X...X",
                "XX.XX",
                "X.X.X",
                "X...X",
                "X...X",
                "X...X"
        };
        String[] N = {
                "X...X",
                "XX..X",
                "X.X.X",
                "X..XX",
                "X...X",
                "X...X"
        };
        String[] O = {
                ".XXX.",
                "X...X",
                "X...X",
                "X...X",
                "X...X",
                ".XXX."
        };
        String[] P = {
                "XXXX.",
                "X...X",
                "XXXX.",
                "X....",
                "X....",
                "X...."
        };
        String[] Q = {
                ".XXX.",
                "X...X",
                "X...X",
                "X...X",
                "X..XX",
                ".XXXX"
        };
        String[] R = {
                "XXXX.",
                "X...X",
                "XXXX.",
                "X..X.",
                "X...X",
                "X...X"
        };
        String[] S = {
                ".XXXX",
                "X....",
                ".XXX.",
                "....X",
                "....X",
                "XXXX."
        };
        String[] T = {
                "XXXXX",
                "..X..",
                "..X..",
                "..X..",
                "..X..",
                "..X.."
        };
        String[] U = {
                "X...X",
                "X...X",
                "X...X",
                "X...X",
                "X...X",
                ".XXX."
        };
        String[] V = {
                "X...X",
                "X...X",
                "X...X",
                "X...X",
                ".X.X.",
                "..X.."
        };
        String[] W = {
                "X...X",
                "X...X",
                "X...X",
                "X.X.X",
                "XX.XX",
                "X...X"
        };
        String[] X = {
                "X...X",
                ".X.X.",
                "..X..",
                ".X.X.",
                "X...X",
                "X...X"
        };
        String[] Y = {
                "X...X",
                "X...X",
                ".X.X.",
                "..X..",
                "..X..",
                "..X.."
        };
        String[] Z = {
                "XXXXX",
                "...X.",
                "..X..",
                ".X...",
                "X....",
                "XXXXX"
        };
        String[] ZERO = {
                ".XXX.",
                "X...X",
                "X..XX",
                "X.X.X",
                "XX..X",
                ".XXX."
        };
        String[] ONE = {
                "..X..",
                ".XX..",
                "..X..",
                "..X..",
                "..X..",
                ".XXX."
        };
        String[] TWO = {
                ".XXX.",
                "X...X",
                "...X.",
                "..X..",
                ".X...",
                "XXXXX"
        };
        String[] THREE = {
                "XXXXX",
                "....X",
                "..XX.",
                "....X",
                "X...X",
                ".XXX."
        };
        String[] FOUR = {
                "...X.",
                "..XX.",
                ".X.X.",
                "X..X.",
                "XXXXX",
                "...X."
        };
        String[] FIVE = {
                "XXXXX",
                "X....",
                "XXXX.",
                "....X",
                "....X",
                "XXXX."
        };
        String[] SIX = {
                ".XXX.",
                "X....",
                "XXXX.",
                "X...X",
                "X...X",
                ".XXX."
        };
        String[] SEVEN = {
                "XXXXX",
                "....X",
                "...X.",
                "..X..",
                ".X...",
                ".X..."
        };
        String[] EIGHT = {
                ".XXX.",
                "X...X",
                ".XXX.",
                "X...X",
                "X...X",
                ".XXX."
        };
        String[] NINE = {
                ".XXX.",
                "X...X",
                "X...X",
                ".XXXX",
                "....X",
                ".XXX."
        };
        String[] PLUS = {
                "..X..",
                "..X..",
                "XXXXX",
                "..X..",
                "..X..",
                "....."
        };
        String[] MINUS = {
                ".....",
                ".....",
                "XXXXX",
                ".....",
                ".....",
                "....."
        };
        String[] STAR = {
                "X...X",
                ".X.X.",
                "..X..",
                ".X.X.",
                "X...X",
                "....."
        };
        String[] SLASH = {
                "....X",
                "...X.",
                "..X..",
                ".X...",
                "X....",
                "....."
        };
        String[] EQUALS = {
                ".....",
                "XXXXX",
                ".....",
                "XXXXX",
                ".....",
                "....."
        };
        String[] QUESTION = {
                ".XXX.",
                "X...X",
                "...X.",
                "..X..",
                ".....",
                "..X.."
        };
        String[] QUOTE = {
                "X.X..",
                "X.X..",
                ".....",
                ".....",
                ".....",
                "....."
        };
        String[] SEMICOLON = {
                ".....",
                "..X..",
                ".....",
                "..X..",
                ".X...",
                "....."
        };
        // (Other symbols like colon, comma, etc., can be defined similarly if needed)

        // Load all defined patterns into fontMap
        addCharPattern('A', A);  addCharPattern('B', B);  addCharPattern('C', C);
        addCharPattern('D', D);  addCharPattern('E', E);  addCharPattern('F', F);
        addCharPattern('G', G);  addCharPattern('H', H);  addCharPattern('I', I);
        addCharPattern('J', J);  addCharPattern('K', K);  addCharPattern('L', L);
        addCharPattern('M', M);  addCharPattern('N', N);  addCharPattern('O', O);
        addCharPattern('P', P);  addCharPattern('Q', Q);  addCharPattern('R', R);
        addCharPattern('S', S);  addCharPattern('T', T);  addCharPattern('U', U);
        addCharPattern('V', V);  addCharPattern('W', W);  addCharPattern('X', X);
        addCharPattern('Y', Y);  addCharPattern('Z', Z);
        addCharPattern('0', ZERO); addCharPattern('1', ONE); addCharPattern('2', TWO);
        addCharPattern('3', THREE); addCharPattern('4', FOUR); addCharPattern('5', FIVE);
        addCharPattern('6', SIX); addCharPattern('7', SEVEN); addCharPattern('8', EIGHT);
        addCharPattern('9', NINE);
        addCharPattern('+', PLUS); addCharPattern('-', MINUS); addCharPattern('*', STAR);
        addCharPattern('/', SLASH); addCharPattern('=', EQUALS);
        addCharPattern('?', QUESTION); addCharPattern('\"', QUOTE);
        addCharPattern(';', SEMICOLON);
        addCharPattern('\'', QUOTE); // use same pattern for apostrophe as single quote (simplification)
        addCharPattern(':', SEMICOLON); // colon can reuse semicolon pattern for now
        addCharPattern('.', new String[]{
                ".....",
                ".....",
                ".....",
                ".....",
                "..X..",
                "..X.."
        });
        addCharPattern(',', new String[]{
                ".....",
                ".....",
                ".....",
                "..X..",
                ".X...",
                "....."
        });
        addCharPattern(' ', new String[]{
                ".....",
                ".....",
                ".....",
                ".....",
                ".....",
                "....."
        });
    }

    /** Utility to convert pattern strings to int matrix and add to fontMap */
    private static void addCharPattern(char ch, String[] pattern) {
        int rows = pattern.length;
        int cols = pattern[0].length();
        // We expect 5 columns in pattern strings and 6 rows in many cases, but let's handle generically
        // We'll create a 2D int array of size [rows][cols] (assuming cols <= 6).
        int drawCols = cols;
        // If cols < 6, we'll pad to 6 columns for our cell width
        if (cols < 6) {
            drawCols = 6;
        }
        int[][] matrix = new int[rows][drawCols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                matrix[y][x] = (pattern[y].charAt(x) == 'X') ? 1 : 0;
            }
            // Pad the remaining columns with 0 if any
            for (int x = cols; x < drawCols; x++) {
                matrix[y][x] = 0;
            }
        }
        fontMap.put(ch, matrix);
    }
}
