import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorWarning;
import edu.mit.csail.sdg.alloy4compiler.ast.Expr;
import edu.mit.csail.sdg.alloy4compiler.ast.Module;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig;
import edu.mit.csail.sdg.alloy4compiler.parser.CompUtil;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Options;
import edu.mit.csail.sdg.alloy4compiler.translator.A4Solution;
import edu.mit.csail.sdg.alloy4compiler.translator.TranslateAlloyToKodkod;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Executor {
    public static void main(String[] args) throws Err {
        createModel();

        args = new String[1];
        args[0] = "C:\\Users\\varuchin.MERA\\Desktop\\alloy\\AlloyCommandline\\fileSystemModel.als";
        A4Reporter rep = new A4Reporter() {
            @Override
            public void warning(ErrorWarning msg) {
                System.out.print("Relevance Warning:\n" + (String.valueOf(msg).trim()) + "\n\n");
                System.out.flush();
            }
        };
        A4Options options = new A4Options();
        options.solver = A4Options.SatSolver.SAT4J;

        StringBuilder successOutput = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        execute(rep, options, errorOutput, successOutput, args);

        if (errorOutput.length() != 0) {
            System.err.print(String.valueOf(errorOutput));
        } else {
            System.out.print(String.valueOf(successOutput));
        }
    }

    private static void execute(A4Reporter rep, A4Options options, StringBuilder errorOutput, StringBuilder successOutput, String... args) {
        Stream.of(args).forEach(filename -> {
            Optional<Module> world = Optional.empty();
            try {
                world = Optional.of(CompUtil.parseEverything_fromFile(rep, null, filename));
            } catch (Exception err) {
                Logger.getAnonymousLogger().log(Level.SEVERE, String.valueOf(err));
            }
            world.ifPresent(w -> w.getAllCommands().forEach(command -> {
                Optional<A4Solution> a4solution = Optional.empty();
                try {
                    a4solution = Optional.of(TranslateAlloyToKodkod.execute_command(rep, w.getAllReachableSigs(), command, options));
                } catch (Err err) {
                    err.printStackTrace();
                }
                if (command.check && a4solution.isPresent()) {
                    if (a4solution.get().satisfiable()) {
                        errorOutput.append("Assertion error in ")
                                .append(command.pos.filename)
                                .append(" at line ")
                                .append(command.pos.y)
                                .append(" column ")
                                .append(command.pos.x)
                                .append(":\n");
                        errorOutput.append("Counter-example of " + command + " found.\n");
                    } else {
                        successOutput.append("Maybe valid: ")
                                .append(command)
                                .append("\n");
                    }
                } else {
                    if (a4solution.get().satisfiable()) {
                        successOutput.append("Consistent: ")
                                .append(command)
                                .append("\n");
                    } else {
                        errorOutput.append("Inconsistent error in ")
                                .append(command.pos.filename)
                                .append(" at line ")
                                .append(command.pos.y)
                                .append(" column ")
                                .append(command.pos.x)
                                .append(":\n");
                        errorOutput.append("No instance of " + command + " found.\n");
                    }
                }
            }));
        });
    }

    private static void createModel() throws Err {
        A4Options a4Options = new A4Options();
        a4Options.solver = A4Options.SatSolver.SAT4J;

        Sig.PrimSig FSObject = new Sig.PrimSig("FSObj");
        Sig.PrimSig Dir = new Sig.PrimSig("Dir", FSObject);
        Sig.PrimSig File = new Sig.PrimSig("File", FSObject);

        Expr parent = FSObject.addField("parent", Dir.lone_arrow_lone(Dir));
        Expr contents = Dir.addField("contents", FSObject.setOf());

        System.out.println();
    }
}
