package sneer.android.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.*;
import sneer.admin.Database;
import sneer.admin.UniqueConstraintViolated;
import sneer.commons.Lists;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class SneerSqliteDatabase implements Closeable, Database {

	private final SQLiteDatabase sqlite;


	public static SneerSqliteDatabase openDatabase(File file) throws IOException {
		return new SneerSqliteDatabase(SQLiteDatabase.openOrCreateDatabase(file,null));
	}


	public SneerSqliteDatabase(SQLiteDatabase sqlite) {
		this.sqlite = sqlite;
	}


	@Override
	public void createTable(String tableName, List<List<Object>> columns) {
		String sql = "CREATE TABLE " + tableName + " (" + columnsString(columns) + ")";
		sqlite.execSQL(sql);
	}


	@Override
	public void createIndex(String table, String indexName, List<String> columns, boolean unique) {
		String sql = "CREATE " + (unique ? "UNIQUE " : "") + "INDEX " + indexName + " ON " + table + " (" + Lists.join(columns, ",") + ")";
		sqlite.execSQL(sql);
	}


	@Override
	public long insert(String tableName, Map<String, Object> values) throws UniqueConstraintViolated {
		try {
			return sqlite.insert(tableName, null, toContentValues(values));
		} catch (SQLiteConstraintException e) {
			// android.database.sqlite.SQLiteConstraintException: columns author, original_id are not unique (code 19)
			if (e.getMessage().contains(" not unique "))
				throw new UniqueConstraintViolated(e.getMessage(), e);
			throw e;
		}
	}


	@Override
	public Iterable<List<?>> query(String sql, final List<Object> args) {
        Cursor cursor = sqlite.rawQueryWithFactory(new SQLiteDatabase.CursorFactory() {
            @Override
            public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
                bindAll(query, args);
                return new SQLiteCursor(masterQuery, editTable, query);
            }
        }, sql, null, null);

		ArrayList<List<?>> ret = new ArrayList<List<?>>(cursor.getCount() + 1);
		ret.add(Arrays.asList(cursor.getColumnNames()));
		if (!cursor.moveToFirst()) return ret;

		do {
			ret.add(row(cursor));
		} while (cursor.moveToNext());

		return ret;
	}

    private void bindAll(SQLiteQuery query, List<Object> args) {
        int i = 1;
        for (Object arg : args) {
            if (arg == null)
                query.bindNull(i);
            else if (arg instanceof Long)
                query.bindLong(i, (Long) arg);
            else if (arg instanceof byte[])
                query.bindBlob(i, (byte[]) arg);
            else
                query.bindString(i, (String) arg);
            i++;
        }
    }


	private ContentValues toContentValues(Map<String, Object> values) {
		ContentValues ret = new ContentValues(values.size());
		for (Map.Entry<String, Object> entry : values.entrySet())
			accumulate(ret, entry.getKey(), entry.getValue());
		return ret;
	}


	private void accumulate(ContentValues cv, String key, Object value) {
		if (value instanceof String) cv.put(key, (String)value);
        else if (value instanceof Long) cv.put(key, (Long)value);
        else cv.put(key, (byte[])value);
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
			if (ret.length() > 0)
				ret += ",";
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
	public void close() throws IOException {
		sqlite.close();
	}

}
