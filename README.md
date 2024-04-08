## Prerequisites
- Java 22
- You need to have a Shared Library named 'SudokuSolver' in a 'lib'-folder (see Repository https://github.com/bkratz/SudokuSolverNative to build this Shared Library)
- You need to copy the file `sudokusolver.h` from repository https://github.com/bkratz/SudokuSolverNative to the 'lib'-folder
- You need to adjust the name of the shared library file in the call to Jextract (if you want to use Jextract generated files)

## Aufruf von Jextract

```shell
<pathToJextract>/jextract 
--output src/main/java 
--target-package <targetPackage> 
--include-typedef Sudoku 
--include-function solve 
--library :lib/libSudokuSolver.dylib 
lib/sudokusolver.h
```
