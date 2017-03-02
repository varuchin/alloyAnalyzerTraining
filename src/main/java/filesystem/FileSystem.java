package filesystem;

import kodkod.ast.Expression;
import kodkod.ast.Formula;
import kodkod.ast.Relation;
import kodkod.ast.Variable;
import kodkod.engine.Solution;
import kodkod.engine.Solver;
import kodkod.engine.fol2sat.HigherOrderDeclException;
import kodkod.engine.fol2sat.UnboundLeafException;
import kodkod.engine.satlab.SATFactory;
import kodkod.instance.Bounds;
import kodkod.instance.TupleFactory;
import kodkod.instance.Universe;

import java.util.ArrayList;
import java.util.List;

public class FileSystem {

    private final Relation Obj, Name, File, Dir, Root, Cur, DirEntry;
    private final Relation entries, parent, name, contents;

    /**
     * Constructs a new instance of the file system problem.
     */
    public FileSystem() {
        Obj = Relation.unary("Object");
        Name = Relation.unary("Name");
        File = Relation.unary("File");
        Dir = Relation.unary("Dir");
        Root = Relation.unary("Root");
        Cur = Relation.unary("Cur");
        DirEntry = Relation.unary("DirEntry");
        entries = Relation.binary("entries");
        parent = Relation.binary("parent");
        name = Relation.binary("name");
        contents = Relation.binary("contents");
    }

    /**
     * Returns the declaration constraints.
     * @return declaration constraints
     */
    public final Formula decls() {
        // File and Dir partition object
        final Formula f0 = Obj.eq(File.union(Dir)).and(File.intersection(Dir).no());
        // Root and Cur are in Dir and do not intersect
        final Formula f1 = Root.in(Dir).and(Cur.in(Dir)).and(Root.intersection(Cur).no());
        // don't need to specify that Dir, Name, and DirEntry are disjoint; implied by bounds
        final Formula f2 = entries.in(Dir.product(DirEntry));
        final Formula f3 = parent.partialFunction(Dir, Dir);
        final Formula f4 = name.function(DirEntry, Name);
        final Formula f5 = contents.function(DirEntry, Obj);
        return f0.and(f1).and(f2).and(f3).and(f4).and(f5);
    }

    /**
     * Returns all facts in the model.
     * @return the facts.
     */
    public final Formula facts() {
        // sig File extends Object {} { some d: Dir | this in d.entries.contents }
        final Variable file = Variable.unary("this");
        final Variable d = Variable.unary("d");
        final Formula f0 = file.in(d.join(entries).join(contents)).forSome(d.oneOf(Dir)).forAll(file.oneOf(File));

//		sig Dir extends Object {
//			  entries: set DirEntry,
//			  parent: lone Dir
//			} {
//			  parent = this.~@contents.~@entries
//			  all e1, e2 : entries | e1.name = e2.name => e1 = e2
//			  this !in this.^@parent
//			  this != Root => Root in this.^@parent
//			}

        final Variable dir = Variable.unary("this");
        final Variable e1 = Variable.unary("e1"), e2 = Variable.unary("e2");

        final Formula f1 = (dir.join(parent)).eq(dir.join(contents.transpose()).join(entries.transpose()));
        final Expression e0 = dir.join(entries);
        final Formula f2 = e1.join(name).eq(e2.join(name)).implies(e1.eq(e2)).forAll(e1.oneOf(e0).and(e2.oneOf(e0)));
        final Formula f3 = dir.in(dir.join(parent.closure())).not();
        final Formula f4 = dir.eq(Root).not().implies(Root.in(dir.join(parent.closure())));
        final Formula f5 = f1.and(f2).and(f3).and(f4).forAll(dir.oneOf(Dir));

//		one sig Root extends Dir {} { no parent }
        final Formula f6 = Root.join(parent).no();

//		sig DirEntry {
//			  name: Name,
//			  contents: Object
//			} { one this.~entries }

        final Variable entry = Variable.unary("this");
        final Formula f7 = entry.join(entries.transpose()).one().forAll(entry.oneOf(DirEntry));

//		fact OneParent {
//		    // all directories besides root xhave one parent
//		    all d: Dir - Root | one d.parent
//		}

        final Formula f8 = d.join(parent).one().forAll(d.oneOf(Dir.difference(Root)));

        return f0.and(f5).and(f6).and(f7).and(f8);
    }

    /**
     * Returns the no aliases assertion.
     * @return the no aliases assertion.
     */
    public final Formula noDirAliases() {
        //all o: Dir | lone o.~contents
        final Variable o = Variable.unary("o");
        return o.join(contents.transpose()).lone().forAll(o.oneOf(Dir));
    }

    /**
     * Returns the formula that 'checks' the noDirAliases assertion.
     * @return decls() and facts() and noDirAliases().not()
     */
    public final Formula checkNoDirAliases() {
        return decls().and(facts()).and(noDirAliases().not());
    }

    /**
     * Returns the bounds for the given scope.
     * @return the bounds for the given scope.
     */
    public final Bounds bounds(int scope) {
        assert scope > 0;
        final int n = scope*3;
        final List<String> atoms = new ArrayList<>(n);
        for(int i = 0; i < scope; i++)
            atoms.add("Object"+i);
        for(int i = 0; i < scope; i++)
            atoms.add("Name"+i);
        for(int i = 0; i < scope; i++)
            atoms.add("DirEntry"+i);

        final Universe u = new Universe(atoms);
        final TupleFactory tupleFactory = u.factory();
        final Bounds bounds = new Bounds(u);

        final int max = scope-1;

        bounds.bound(Obj, tupleFactory.range(tupleFactory.tuple("Object0"), tupleFactory.tuple("Object"+max)));
        bounds.boundExactly(Root, tupleFactory.setOf("Object0"));
        bounds.bound(Cur, bounds.upperBound(Obj));
        bounds.bound(File, bounds.upperBound(Obj));
        bounds.bound(Dir, bounds.upperBound(Obj));
        bounds.bound(Name, tupleFactory.range(tupleFactory.tuple("Name0"), tupleFactory.tuple("Name"+max)));
        bounds.bound(DirEntry, tupleFactory.range(tupleFactory.tuple("DirEntry0"), tupleFactory.tuple("DirEntry"+max)));

        bounds.bound(entries, bounds.upperBound(Dir).product(bounds.upperBound(DirEntry)));
        bounds.bound(parent, bounds.upperBound(Dir).product(bounds.upperBound(Dir)));
        bounds.bound(name, bounds.upperBound(DirEntry).product(bounds.upperBound(Name)));
        bounds.bound(contents, bounds.upperBound(DirEntry).product(bounds.upperBound(Obj)));

        return bounds;
    }

    private static void usage() {
        System.out.println("java examples.FileSystem [scope]");
        System.exit(1);
    }

    /**
     * Usage: java examples.alloy.FileSystem [scope]
     */
    public  static void main(String[] args) {
        args = new String[100];
        if (args.length < 1)
            usage();
        try {
            final int n = Integer.parseInt("3");
            final FileSystem model = new FileSystem();
            final Formula f = model.checkNoDirAliases();
            System.out.println(f);
            final Bounds b = model.bounds(n);
            final Solver solver = new Solver();
            solver.options().setSolver(SATFactory.MiniSat);

            final Solution s = solver.solve(f, b);
            System.out.println(s);


        } catch (NumberFormatException nfe) {
            usage();
        } catch (HigherOrderDeclException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnboundLeafException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
