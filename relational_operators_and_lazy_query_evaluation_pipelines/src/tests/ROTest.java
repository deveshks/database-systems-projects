package tests;

import global.AttrOperator;
import global.AttrType;
import global.RID;
import global.SearchKey;
import heap.HeapFile;
import index.HashIndex;
import relop.FileScan;
import relop.HashJoin;
import relop.IndexScan;
import relop.KeyScan;
import relop.Predicate;
import relop.Projection;
import relop.Schema;
import relop.Selection;
import relop.SimpleJoin;
import relop.Tuple;

/**
 * Test suite for the relop layer.
 */
class ROTest extends TestDriver {

	/** The display name of the test suite. */
	private static final String TEST_NAME = "relational operator tests";

	/** Size of tables in test3. */
	private static final int SUPER_SIZE = 2000;

	/** Drivers table schema. */
	private static Schema s_drivers;

	/** Rides table schema. */
	private static Schema s_rides;

	/** Groups table schema. */
	private static Schema s_groups;

	// --------------------------------------------------------------------------

	/**
	 * Test application entry point; runs all tests.
	 */
	public static void main(String argv[]) {

		// create a clean Minibase instance
		ROTest rot = new ROTest();
		rot.create_minibase();

		// initialize schema for the "Drivers" table
		s_drivers = new Schema(5);
		s_drivers.initField(0, AttrType.INTEGER, 4, "DriverId");
		s_drivers.initField(1, AttrType.STRING, 20, "FirstName");
		s_drivers.initField(2, AttrType.STRING, 20, "LastName");
		s_drivers.initField(3, AttrType.FLOAT, 4, "Age");
		s_drivers.initField(4, AttrType.INTEGER, 4, "NumSeats");

		// initialize schema for the "Rides" table
		s_rides = new Schema(4);
		s_rides.initField(0, AttrType.INTEGER, 4, "DriverId");
		s_rides.initField(1, AttrType.INTEGER, 4, "GroupId");
		s_rides.initField(2, AttrType.STRING, 10, "FromDate");
		s_rides.initField(3, AttrType.STRING, 10, "ToDate");

		// initialize schema for the "Groups" table
		s_groups = new Schema(2);
		s_groups.initField(0, AttrType.INTEGER, 4, "GroupId");
		s_groups.initField(1, AttrType.STRING, 10, "Country");

		// run all the test cases
		System.out.println("\n" + "Running " + TEST_NAME + "...");
		boolean status = PASS;
		status &= rot.test1();
		status &= rot.test2();
		status &= rot.test3();

		// display the final results
		System.out.println();
		if (status != PASS) {
			System.out.println("Error(s) encountered during " + TEST_NAME + ".");
		} else {
			System.out.println("All " + TEST_NAME
					+ " completed; verify output for correctness.");
		}

	} // public static void main (String argv[])

	/**
	 * 
	 */
	protected boolean test1() {
		try {

			System.out.println("\nTest 1: Primative relational operators");
			initCounts();
			saveCounts(null);

			// create and populate a temporary Drivers file and index
			Tuple tuple = new Tuple(s_drivers);
			HeapFile file = new HeapFile(null);
			HashIndex index = new HashIndex(null);
			for (int i = 1; i <= 10; i++) {

				// create the tuple
				tuple.setIntFld(0, i);
				tuple.setStringFld(1, "f" + i);
				tuple.setStringFld(2, "l" + i);
				Float age = (float) (i * 7.7);
				tuple.setFloatFld(3, age);
				tuple.setIntFld(4, i + 100);

				// insert the tuple in the file and index
				RID rid = file.insertRecord(tuple.getData());
				index.insertEntry(new SearchKey(age), rid);

			} // for
			saveCounts("insert");

			// test index scan
			saveCounts(null);
			System.out.println("\n  ~> test key scan (Age = 53.9)...\n");
			SearchKey key = new SearchKey(53.9F);
			KeyScan keyscan = new KeyScan(s_drivers, index, key, file);
			keyscan.execute();
			saveCounts("ixscan");

			// test selection operator
			saveCounts(null);
			System.out.println("\n  ~> test selection (Age > 65 OR Age < 15)...\n");
			Predicate[] preds = new Predicate[] {
					new Predicate(AttrOperator.GT, AttrType.FIELDNO, 3, AttrType.FLOAT,
							65F),
							new Predicate(AttrOperator.LT, AttrType.FIELDNO, 3, AttrType.FLOAT,
									15F) };
			FileScan scan = new FileScan(s_drivers, file);
			Selection sel = new Selection(scan, preds);
			sel.execute();
			saveCounts("select");

			// test projection operator
			saveCounts(null);
			System.out.println("\n  ~> test projection (columns 3 and 1)...\n");
			scan = new FileScan(s_drivers, file);
			Projection pro = new Projection(scan, 3, 1);
			pro.execute();
			saveCounts("project");

			// test simple pipelining
			saveCounts(null);
			System.out.println("\n  ~> selection and projection (pipelined)...\n");
			scan = new FileScan(s_drivers, file);
			sel = new Selection(scan, preds);
			pro = new Projection(sel, 3, 1);
			pro.execute();
			saveCounts("both");

			// test join operator
			saveCounts(null);
			System.out.println("\n  ~> test simple (nested loops) join...\n");
			preds = new Predicate[] { new Predicate(AttrOperator.EQ,
					AttrType.FIELDNO, 0, AttrType.FIELDNO, 5) };
			SimpleJoin join = new SimpleJoin(new FileScan(s_drivers, file),
					new FileScan(s_drivers, file), preds);
			pro = new Projection(join, 0, 1, 5, 6);
			pro.execute();

			// destroy temp files before doing final counts
			join = null;
			pro = null;
			sel = null;
			scan = null;
			keyscan = null;
			index = null;
			file = null;
			System.gc();
			saveCounts("join");

			// that's all folks!
			System.out.print("\n\nTest 1 completed without exception.");
			return PASS;

		} catch (Exception exc) {

			exc.printStackTrace(System.out);
			System.out.print("\n\nTest 1 terminated because of exception.");
			return FAIL;

		} finally {
			printSummary(6);
			System.out.println();
		}
	} // protected boolean test1()

	/**
	 * SELECT * FROM Drivers D INNER JOIN Rides R ON (D.DriverId = R.DriverId);
	 */
	protected boolean test2() {
		try {

			System.out.println("\nTest 2: Hash-based join operator\n");
			initCounts();

			// create and populate the drivers table
			saveCounts(null);
			HeapFile drivers = new HeapFile(null);
			Tuple tuple = new Tuple(s_drivers);
			tuple.setAllFields(1, "Ahmed", "Elmagarmid", 25F, 5);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(2, "Walid", "Aref", 27F, 13);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(3, "Christopher", "Clifton", 18F, 4);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(4, "Sunil", "Prabhakar", 22F, 7);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(5, "Elisa", "Bertino", 26F, 5);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(6, "Susanne", "Hambrusch", 23F, 3);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(7, "David", "Eberts", 24F, 8);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(8, "Arif", "Ghafoor", 20F, 5);
			tuple.insertIntoFile(drivers);
			tuple.setAllFields(9, "Jeff", "Vitter", 19F, 10);
			tuple.insertIntoFile(drivers);
			saveCounts("drivers");

			// create and populate the rides table
			saveCounts(null);
			HeapFile rides = new HeapFile(null);
			tuple = new Tuple(s_rides);
			tuple.setAllFields(3, 5, "2/10/2006", "2/13/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(1, 2, "2/12/2006", "2/14/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(9, 1, "2/15/2006", "2/15/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(5, 7, "2/14/2006", "2/18/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(1, 3, "2/15/2006", "2/16/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(2, 6, "2/17/2006", "2/20/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(3, 4, "2/18/2006", "2/19/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(4, 1, "2/19/2006", "2/19/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(2, 7, "2/18/2006", "2/23/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(8, 5, "2/20/2006", "2/22/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(3, 2, "2/24/2006", "2/26/2006");
			tuple.insertIntoFile(rides);
			tuple.setAllFields(6, 6, "2/25/2006", "2/26/2006");
			tuple.insertIntoFile(rides);
			saveCounts("rides");

			// test hash join operator
			saveCounts(null);
			HashJoin join = new HashJoin(new FileScan(s_drivers, drivers),
					new FileScan(s_rides, rides), 0, 0);
			join.execute();

			// destroy temp files before doing final counts
			join = null;
			rides = null;
			drivers = null;
			System.gc();
			saveCounts("h_join");

			// that's all folks!
			System.out.print("\n\nTest 2 completed without exception.");
			return PASS;

		} catch (Exception exc) {

			exc.printStackTrace(System.out);
			System.out.print("\n\nTest 2 terminated because of exception.");
			return FAIL;

		} finally {
			printSummary(3);
			System.out.println();
		}
	} // protected boolean test2()

	/**
	 * SELECT * FROM Groups G INNER JOIN Rides R ON (G.GroupId = R.GroupId) INNER
	 * JOIN Drivers D ON (R.DriverId = D.DriverId) WHERE D.NumSeats < G.GroupId;
	 */
	protected boolean test3() {
		try {

			System.out.println("\nTest 3: The most complex query yet!\n");
			initCounts();

			// create and populate a temporary Drivers file and index
			saveCounts(null);
			Tuple tuple = new Tuple(s_drivers);
			HeapFile drivers = new HeapFile(null);
			HashIndex ixdrivers = new HashIndex(null);
			for (int i = 1; i <= SUPER_SIZE; i++) {

				// create the tuple
				tuple.setIntFld(0, i);
				tuple.setStringFld(1, "f" + i);
				tuple.setStringFld(2, "l" + i);
				tuple.setFloatFld(3, (float) (i * 7.7));
				tuple.setIntFld(4, i + 100);

				// insert the tuple in the file and index
				RID rid = drivers.insertRecord(tuple.getData());
				ixdrivers.insertEntry(new SearchKey(i), rid);

			} // for
			saveCounts("drivers");

			// create and populate the rides table
			saveCounts(null);
			initRandom();
			tuple = new Tuple(s_rides);
			HeapFile rides = new HeapFile(null);
			for (int i = 1; i <= SUPER_SIZE; i++) {
				// random relationships between drivers and groups
				int r1 = Math.abs(random.nextInt() % SUPER_SIZE + 1);
				int r2 = Math.abs(random.nextInt() % (SUPER_SIZE / 10) + 1);
				tuple.setAllFields(r1, r2, "3/27/2006", "4/7/2006");
				tuple.insertIntoFile(rides);
			}
			saveCounts("rides");

			// create and populate the groups table
			saveCounts(null);
			tuple = new Tuple(s_groups);
			HeapFile groups = new HeapFile(null);
			for (int i = 1; i <= SUPER_SIZE / 10; i++) {
				tuple.setAllFields(i, "Purdue");
				tuple.insertIntoFile(groups);
			}
			saveCounts("groups");

			// hash join of hash join; selection for output's sake
			saveCounts(null);
			HashJoin join1 = new HashJoin(new FileScan(s_groups, groups),
					new FileScan(s_rides, rides), 0, 1);
			HashJoin join2 = new HashJoin(join1, new IndexScan(s_drivers, ixdrivers,
					drivers), 2, 0);
			Selection sel = new Selection(join2, new Predicate(AttrOperator.LT,
					AttrType.FIELDNO, 10, AttrType.FIELDNO, 0));
			sel.execute();

			// destroy temp files before doing final counts
			sel = null;
			join2 = null;
			join1 = null;
			groups = null;
			rides = null;
			ixdrivers = null;
			drivers = null;
			System.gc();
			saveCounts("query");

			// that's all folks!
			System.out.print("\n\nTest 3 completed without exception.");
			return PASS;

		} catch (Exception exc) {

			exc.printStackTrace(System.out);
			System.out.print("\n\nTest 3 terminated because of exception.");
			return FAIL;

		} finally {
			printSummary(4);
			System.out.println();
		}
	} // protected boolean test3()

} // class ROTest extends TestDriver
