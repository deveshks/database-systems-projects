package tests;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
class QEPTest extends TestDriver {

	/** The display name of the test suite. */
	private static final String TEST_NAME = "relational operator tests";

	/** Size of tables in test3. */
	private static final int SUPER_SIZE = 2000;

	/** Department table schema. */
	private static Schema s_dep;

	/** Employee table schema. */
	private static Schema s_emp;
	
	/** Employee heap file. */
	private static HeapFile emphf;
	private static HeapFile dephf;

	// --------------------------------------------------------------------------

	/**
	 * Test application entry point; runs all tests.
	 */
	public static void main(String argv[]) {

		// create a clean Minibase instance
		QEPTest qept = new QEPTest();
		qept.create_minibase();

		// create heap file
		emphf = new HeapFile("EmployeeHeapFile");
		dephf = new HeapFile("DepartmentHeapFile");
		
		// initialize schema for the "Department" table
		s_dep = new Schema(4);
		s_dep.initField(0, AttrType.INTEGER, 4, "DeptId");
		s_dep.initField(1, AttrType.STRING, 20, "Name");
		s_dep.initField(2, AttrType.INTEGER, 10, "MinSalary");
		s_dep.initField(3, AttrType.INTEGER, 10, "MaxSalary");

		// initialize schema for the "Employee" table
		s_emp = new Schema(5);
		s_emp.initField(0, AttrType.INTEGER, 4, "EmpId");
		s_emp.initField(1, AttrType.STRING, 20, "Name");
		s_emp.initField(2, AttrType.INTEGER, 4, "Age");
		s_emp.initField(3, AttrType.INTEGER, 10, "Salary");
		s_emp.initField(4, AttrType.INTEGER, 4, "DeptId");

		// read files for populating tables
		try {
			Tuple tuple;
			String[] empFields = new String[5];
			String[] depFields = new String[4];
			String line;
			
			BufferedReader empbr = new BufferedReader(new FileReader(argv[0]+"/Employee.txt"));
			line = empbr.readLine();
			
			while ((line = empbr.readLine()) != null) {
				empFields = line.split(",");
				for(int i=0;i<5;i++){
					empFields[i] = empFields[i].trim();
				}
				tuple = new Tuple(s_emp);
				tuple.setAllFields(Integer.parseInt(empFields[0]), empFields[1], Integer.parseInt(empFields[2]),
						Integer.parseInt(empFields[3]), Integer.parseInt(empFields[4]));
				tuple.insertIntoFile(emphf);
			}
			
			empbr.close();
			
			BufferedReader depbr = new BufferedReader(new FileReader(argv[0]+"/Department.txt"));
			line = depbr.readLine();
			
			while ((line = depbr.readLine()) != null) {
				depFields = line.split(",");
				for(int i=0;i<4;i++){
					depFields[i] = depFields[i].trim();
				}
				tuple = new Tuple(s_dep);
				tuple.setAllFields(Integer.parseInt(depFields[0]), depFields[1], Integer.parseInt(depFields[2]),
						Integer.parseInt(depFields[3]));
				tuple.insertIntoFile(dephf);
			}
			
			depbr.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// run all the test cases
		System.out.println("\n" + "Running " + TEST_NAME + "...");
		boolean status = PASS;
		
		 status &= qept.test1(); 
		 status &= qept.test2(); 
		 status &= qept.test3(); 
		 status &= qept.test4();
		 
		// display the final results
		System.out.println();
		if (status != PASS) {
			System.out.println("Error(s) encountered during " + TEST_NAME + ".");
		} else {
			System.out.println("All " + TEST_NAME + " completed; verify output for correctness.");
		}

	}

	/* Select EmpId,Name,Age From Employee */
	protected boolean test1() {

		try {

			System.out.println("\nTest Query 1");
			
			// execute query
			Projection pro = new Projection(new FileScan(s_emp,emphf), 0, 1, 2);
			pro.execute();

			return PASS;

		} catch (Exception exc) {
			return FAIL;

		} finally {
			System.out.println();
		}
	}

	/* Select Name From Department Where MinSalary = MaxSalary */
	protected boolean test2() {

		try {

			System.out.println("\nTest Query 2");
			// execute query
			Predicate[] preds = new Predicate[] {
					new Predicate(AttrOperator.EQ, AttrType.FIELDNO, 2, AttrType.FIELDNO, 3) };
			Selection sel = new Selection(new FileScan(s_dep,dephf), preds);
			Projection pro = new Projection(sel, 1);
			pro.execute();
			return PASS;

		} catch (Exception exc) {
			return FAIL;

		} finally {
			System.out.println();
		}
	}

	/*
	 * Select Name, Department.Name, Department.MaxSalary From Employee Join
	 * Department On Employee.DeptId = Department.DeptId
	 */
	protected boolean test3() {

		try {

			System.out.println("\nTest Query 3");

			// execute query
			HashJoin join = new HashJoin(new FileScan(s_emp,emphf),new FileScan(s_dep,dephf), 4,
					0);
			Projection pro = new Projection(join, 1, 6, 8);
			pro.execute();

			return PASS;

		} catch (Exception exc) {
			return FAIL;

		} finally {
			System.out.println();
		}
	}

	/*
	 * Select Name,Salary from Employee From Employee Join Department On
	 * Employee.DeptId = Department.DeptId Where Employee.Salary >
	 * Department.MaxSalary
	 */

	protected boolean test4() {

		try {

			System.out.println("\nTest Query 4");

			// execute query
			HashJoin join = new HashJoin(new FileScan(s_emp,emphf), new FileScan(s_dep,dephf), 4,
					0);
			Selection sel = new Selection(join,
					new Predicate(AttrOperator.GT, AttrType.FIELDNO, 3, AttrType.FIELDNO, 8));
			Projection pro = new Projection(sel, 1);
			pro.execute();

			return PASS;

		} catch (Exception exc) {
			return FAIL;

		} finally {
			System.out.println();
		}
	}
}