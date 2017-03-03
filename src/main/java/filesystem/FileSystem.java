package filesystem;

import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;
import kodkod.util.nodes.PrettyPrinter;

import java.util.Arrays;

public class FileSystem {
    private final Relation file;
    private final Relation dir;
    private final Relation root;
    private final Relation contents;

    private static final String DIR0 = "dir0";
    private static final String DIR1 = "dir1";
    private static final String FILE0 = "file0";
    private static final String FILE1 = "file1";
    private static final String FILE2 = "file2";

    public FileSystem() {
        this.file = Relation.unary("File");
        this.dir = Relation.unary("Dir");
        this.root = Relation.unary("Root");
        this.contents = Relation.binary("contents");
    }

    public Formula defineConstraints() {
        Formula fileSystemContent = contents.in(dir.product((dir.union(file))));

        final Variable parent = Variable.unary("parent");
        Formula singleParent = parent.in(parent.join(contents.closure())).not().forAll(parent.oneOf(dir));

        Formula rootDir = root.in(dir);

        Formula correctPath = file.union(dir).in(root.join(contents.reflexiveClosure()));

        return Formula.and(Arrays.asList(fileSystemContent, singleParent, rootDir, correctPath));
    }

    public Bounds defineBounds() {
        final Universe universe = new Universe(DIR0, DIR1, FILE0, FILE1, FILE2);
        final Bounds bounds = new Bounds(universe);
        final TupleFactory tupleFactory = universe.factory();

        bounds.boundExactly(root, tupleFactory.setOf(DIR0));
        bounds.bound(dir, tupleFactory.setOf(DIR0, DIR1));
        bounds.bound(file, tupleFactory.setOf(FILE0, FILE1, FILE2));
        bounds.bound(contents, tupleFactory.setOf(tupleFactory.tuple(DIR0, DIR1)),
                bounds.upperBound(dir).product(tupleFactory.allOf(1)));

        return bounds;
    }

    public static void main(String[] args) {
        final FileSystem fileSys = new FileSystem();
        final Formula constraints = fileSys.defineConstraints();

        System.out.println(PrettyPrinter.print(constraints, 2));

        final Bounds bounds = fileSys.defineBounds();
        final Solver solver = new Solver();

        solver.options().setSolver(SATFactory.DefaultSAT4J);

        final Solution solution = solver.solve(constraints, bounds);

        System.out.println(solution);
    }

}
