package sneer.android.main.core;
import java.io.*;
import java.util.*;
import java.util.Arrays;

import sneer.commons.*;
import sneer.core.*;
import sneer.persistent_tuple_base.*;
import android.annotation.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;


public class SneerSqliteDatabase implements Database, Disposable {
	
	public static SneerSqliteDatabase openDatabase(File file) throws IOException {
		return new SneerSqliteDatabase(
			SQLiteDatabase.openOrCreateDatabase(file,null));
	}

	private SQLiteDatabase sqlite;

	public SneerSqliteDatabase(SQLiteDatabase sqlite) {
		this.sqlite = sqlite;
	}

	
	@Override
	public Object db_create_table(Object tableName, Object columns) {
		String sql = "CREATE TABLE " + name(tableName) + " (" + columnsString((List<?>)columns) + ")";
		sqlite.execSQL(sql);
		return null;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public Object db_insert(Object tableName, Object values) {
		return sqlite.insert(name(tableName), null, toContentValues((Map<String, Object>)values));
	}

	
	private ContentValues toContentValues(Map<String, Object> values) {
		ContentValues ret = new ContentValues(values.size());
		for (Map.Entry<String, Object> entry : values.entrySet())
			accumulate(ret, entry.getKey(), entry.getValue());
		return ret;
	}


	private void accumulate(ContentValues cv, String key, Object value) {
		if (value instanceof String) cv.put(key, (String)value);
		else cv.put(key, (byte[])value);
	}


	@Override
	public Object db_query(Object sqlAndParams) {
		String sql = (String)((List<?>)sqlAndParams).get(0);
		Cursor cursor = sqlite.rawQuery(sql, null);
		ArrayList<List<?>> ret = new ArrayList<List<?>>(cursor.getCount() + 1);
		ret.add(Arrays.asList(cursor.getColumnNames()));
		if (!cursor.moveToFirst()) return ret;
		
		do {
			ret.add(row(cursor));
		} while (cursor.moveToNext());

		return ret;
	}

	
	private List<?> row(Cursor cursor) {
		List<Object> ret = new ArrayList<Object>(cursor.getColumnCount());
		for (int i = 0; i < cursor.getColumnCount(); i++)
			ret.add(valueOf(cursor, i));
		return ret;
	}


	private Object valueOf(Cursor cursor, int col) {
		switch (cursor.getType(col)) {
		case Cursor.FIELD_TYPE_STRING : return cursor.getString(col);
		case Cursor.FIELD_TYPE_BLOB : return cursor.getBlob(col);
		case Cursor.FIELD_TYPE_FLOAT : return cursor.getDouble(col);
		case Cursor.FIELD_TYPE_INTEGER : return cursor.getLong(col);
		case Cursor.FIELD_TYPE_NULL : return null;
		}
		throw new IllegalStateException();
	}


	private String columnsString(List<?> columns) {
		String ret = "";
		for (Object column : columns) {
			if (ret.length() > 0) ret += ",";
			ret += columnString((List<?>)column);
		}
		return ret;
	}


	private String columnString(List<?> column) {
		return columnName(column) + " " + columnType(column) + " " + columnModifiers(column);
	}


	private String columnModifiers(List<?> column) {
		return Lists.join(column.subList(2, column.size()), " ");
	}


	@SuppressLint("DefaultLocale")
	private String columnType(List<?> column) {
		return name(column.get(1)).toUpperCase();
	}


	private String columnName(List<?> column) {
		return name(column.get(0));
	}


	private String name(Object keyword) {
		return keyword.toString().substring(1);
	}


	@Override
	public Object dispose() {
		sqlite.close();
		return null;
	}


}
