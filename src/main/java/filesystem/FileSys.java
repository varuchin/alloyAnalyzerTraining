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

import java.util.ArrayList;
import java.util.List;

public class FileSys {

//    private final Relation fsObject ;
//    private final Relation dir;
//    private final Relation file;
//
//    private final Variable contents;
//    private final Relation parent;
//
//    public FileSys() {
//        fsObject = Relation.unary("FSObject");
//        contents = Variable.unary("contents");
//        parent = Relation.unary("parent");
//        dir = Relation.unary("dir");
//        file = Relation.unary("file");
//    }
//
//    private final Formula declare() {
//        final Formula fsObjectFormula = fsObject.in(dir.product(parent)).and(dir.lone());
//
////                fsObject.eq(file.union(dir)).and(file.intersection(dir).no());
//        final Formula dirFormula = dir.eq()
//    }
//
//    private final Variable defineVariables() {
//
//    }
private final Relation file, dir, root;
    private final Relation contents;

    /**
     * Constructs a new instance of the file system problem.
     */
    public FileSys() {
        file = Relation.unary("File");
        dir = Relation.unary("Dir");
        root = Relation.unary("Root");
        contents = Relation.binary("contents");
    }

    /**
     * Returns the toy filesystem constraints
     * @return toy filesystem constraints
     */
    public Formula constraints() {
        final List<Formula> formulas = new ArrayList<Formula>();

        formulas.add( contents.in(dir.product(dir.union(file))) );

        final Variable d = Variable.unary("d");
        formulas.add( d.in(d.join(contents.closure())).not().forAll(d.oneOf(dir)) );

        formulas.add( root.in(dir) );

        formulas.add( file.union(dir).in(root.join(contents.reflexiveClosure())) );

        return Formula.and(formulas);
    }

    /**
     * Returns the toy filesystem bounds.
     * @return toy filesystem bounds
     */
    public final Bounds bounds() {
        final Universe universe = new Universe("d0", "d1", "f0", "f1", "f2");
        final Bounds bounds = new Bounds(universe);
        final TupleFactory factory = universe.factory();
        bounds.boundExactly(root, factory.setOf("d0"));
        bounds.bound(dir, factory.setOf("d0", "d1"));
        bounds.bound(file, factory.setOf("f0", "f1", "f2"));
        bounds.bound(contents, factory.setOf(factory.tuple("d0", "d1")), bounds.upperBound(dir).product(factory.allOf(1)));
        return bounds;
    }

    /**
     * Usage: java examples.alloy.FileSys
     */
    public  static void main(String[] args) {
        args = new String[100];
        final FileSys toy = new FileSys();
        final Formula formula = toy.constraints();
        System.out.println(PrettyPrinter.print(formula, 2));
        final Bounds bounds = toy.bounds();
        final Solver solver = new Solver();
        solver.options().setSolver(SATFactory.MiniSat);

        final Solution s = solver.solve(formula, bounds);
        System.out.println(s);
    }



}
