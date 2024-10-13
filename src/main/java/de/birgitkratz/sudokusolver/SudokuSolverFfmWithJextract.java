package de.birgitkratz.sudokusolver;

import de.birgitkratz.sudokusolver.jextract.sudoku;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

import static de.birgitkratz.sudokusolver.jextract.sudoku.N;
import static de.birgitkratz.sudokusolver.jextract.sudoku.N2;
import static de.birgitkratz.sudokusolver.jextract.sudoku.a;
import static de.birgitkratz.sudokusolver.jextract.sudokusolver_h.C_CHAR;
import static de.birgitkratz.sudokusolver.jextract.sudokusolver_h.solve;

public class SudokuSolverFfmWithJextract {
    public static void main(String[] args) throws IOException {
        // 1. Allocate on-heap memory to store Sudoku input
        byte[] board = readInBoard();

        // 2. Use try-with-resources to manage the lifetime of off-heap memory
        try(Arena arena = Arena.ofConfined()) {
            // 3. Allocate a region of off-heap memory to store the input parameter
            // includes describing the input parameter of the solve-Method as a MemoryLayout
            final var solveMemorySegment = sudoku.allocate(arena);

            // 4. Allocate a region of off-heap memory initialized with the elements of the board array
            final var structMemorySegment = arena.allocateFrom(C_CHAR, board);

            // 5. Set the values of the input struct
            a(solveMemorySegment, structMemorySegment);
            N(solveMemorySegment, (byte) 9);
            N2(solveMemorySegment, (byte) 3);

            // 6. Call the foreign function (includes finding foreign function on the C library path and get a MethodHandle)
            var solved = solve(solveMemorySegment);

            if (solved) {
                final var resultSegment = a(solveMemorySegment);
                int[] solvedSudoku = new int[board.length];

                // 7. Copy the solved sudoku board from off-heap to on-heap
                for (int i = 0; i < board.length; ++i) {
                    solvedSudoku[i] = resultSegment.get(C_CHAR, i);
                }

                System.out.println("This was easy!");
                writeOutSolvedBoard(solvedSudoku);
            } else {
                System.out.println("This was a little too hard.");
            }
        } // 8. All off-heap memory is deallocated here
    }

    private static byte[] readInBoard() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final var is = classloader.getResourceAsStream("input.txt");

        final var board = new byte[81];

        try (InputStreamReader streamReader =
                     new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            int lineCounter = 0;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.startsWith("-")) {
                    continue;
                }
                line = line.replace("_", "0");
                line = line.replace("| ", "");
                final var split = line.split(" ");
                int finalLineCounter = lineCounter;
                IntStream.range(0, split.length)
                                .forEach(idx -> board[finalLineCounter * split.length + idx] = Byte.parseByte(split[idx]));
                lineCounter++;
            }
        }
        return board;
    }

    private static void writeOutSolvedBoard(int[] solution) {
        IntStream.range(0, 9).forEach(i -> {
            IntStream.range(0, 3).forEach(j -> {
                final var list = Arrays.stream(solution).skip(i * 9L + j * 3L).limit(3)
                        .boxed().toList();
                final var output = list.toString()
                        .replace(", ", " ")
                        .replace("[", "")
                        .replace("]", " | ");
                System.out.print(output);
            });
            if ((i + 1) % 3 == 0) {
                System.out.println("\n-----------------------");
            } else {
                System.out.println();
            }
        });
    }
}
