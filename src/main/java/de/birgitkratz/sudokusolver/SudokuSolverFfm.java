package de.birgitkratz.sudokusolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class SudokuSolverFfm {
    static {
        System.loadLibrary("SudokuSolver");
    }

    public static void main(String[] args) throws IOException {
        // 1. Allocate on-heap memory to store Sudoku input
        byte[] board = readInBoard();

        // 2. Find foreign function on the C library path and get a MethodHandle
        final var symbolLookup = SymbolLookup.loaderLookup().find("solve").orElseThrow();
        final var solve = Linker.nativeLinker().downcallHandle(symbolLookup, FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS));

        // 3. Describe the input parameter of the solve-Method as a MemoryLayout
        final var sudokuStructLayout = MemoryLayout.structLayout(
                ADDRESS.withTargetLayout(
                        MemoryLayout.sequenceLayout(board.length, ValueLayout.JAVA_BYTE)
                ).withName("a"),
                ValueLayout.JAVA_BYTE.withName("N"),
                ValueLayout.JAVA_BYTE.withName("N2")

        );

        // 4. Getting VarHandles for the MemoryLayout (which internally handles offsets)
        final var aHandle = sudokuStructLayout.varHandle(MemoryLayout.PathElement.groupElement("a"));
        final var nHandle = sudokuStructLayout.varHandle(MemoryLayout.PathElement.groupElement("N"));
        final var n2Handle = sudokuStructLayout.varHandle(MemoryLayout.PathElement.groupElement("N2"));

        // 5. Use try-with-resources to manage the lifetime of off-heap memory
        try (Arena arena = Arena.ofConfined()) {
            // 6. Allocate a region of off-heap memory to store the input parameter
            final var solveMemorySegment = arena.allocate(sudokuStructLayout);

            // 7. Allocate a region of off-heap memory initialized with the elements of the board array
            final var boardMemorySegment = arena.allocateFrom(JAVA_BYTE, board);

            // 8. Set the values of the input struct
            aHandle.set(solveMemorySegment, 0L, boardMemorySegment);
            nHandle.set(solveMemorySegment, 0L, (byte) 9);
            n2Handle.set(solveMemorySegment, 0L, (byte) 3);

            // Alternative solution for setting the values, with self-handling of offsets
            // solveMemorySegment.set(ADDRESS, 0L, boardMemorySegment);
            // solveMemorySegment.set(JAVA_BYTE, ADDRESS.byteSize(), (byte) 9);
            // solveMemorySegment.set(JAVA_BYTE, ADDRESS.byteSize() + JAVA_BYTE.byteSize(), (byte) 3);

            // 9. Call the foreign function
            final var solved = (boolean) solve.invoke(solveMemorySegment);

            if (solved) {
                int[] solvedSudoku = new int[board.length];
                // 10. Copy the solved sudoku board from off-heap to on-heap
                for (int i = 0; i < board.length; ++i) {
                    solvedSudoku[i] = boardMemorySegment.get(JAVA_BYTE, i);
                }

                System.out.println("Successfully solved this Sudoku.");
                writeOutSolvedBoard(solvedSudoku);
            } else {
                System.out.println("This was a little too hard.");
            }
        } catch (Throwable e) {
            System.out.println("I could not solve this one :(");
        }
    } // 11. All off-heap memory is deallocated here

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
