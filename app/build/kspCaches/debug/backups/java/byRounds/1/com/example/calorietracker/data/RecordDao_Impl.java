package com.example.calorietracker.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class RecordDao_Impl implements RecordDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyRecordEntity> __insertionAdapterOfDailyRecordEntity;

  private final EntityInsertionAdapter<DailyRecordEntity> __insertionAdapterOfDailyRecordEntity_1;

  private final EntityInsertionAdapter<CalorieItemEntity> __insertionAdapterOfCalorieItemEntity;

  private final EntityDeletionOrUpdateAdapter<CalorieItemEntity> __deletionAdapterOfCalorieItemEntity;

  private final EntityDeletionOrUpdateAdapter<DailyRecordEntity> __updateAdapterOfDailyRecordEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteItemById;

  public RecordDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyRecordEntity = new EntityInsertionAdapter<DailyRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `daily_records` (`date`,`weight`,`totalIntake`,`totalBurned`,`netCalories`,`totalCarbs`,`totalProtein`,`totalFat`,`totalWater`,`sleepDuration`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyRecordEntity entity) {
        statement.bindString(1, entity.getDate());
        if (entity.getWeight() == null) {
          statement.bindNull(2);
        } else {
          statement.bindDouble(2, entity.getWeight());
        }
        statement.bindLong(3, entity.getTotalIntake());
        statement.bindLong(4, entity.getTotalBurned());
        statement.bindLong(5, entity.getNetCalories());
        statement.bindLong(6, entity.getTotalCarbs());
        statement.bindLong(7, entity.getTotalProtein());
        statement.bindLong(8, entity.getTotalFat());
        statement.bindLong(9, entity.getTotalWater());
        statement.bindLong(10, entity.getSleepDuration());
      }
    };
    this.__insertionAdapterOfDailyRecordEntity_1 = new EntityInsertionAdapter<DailyRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_records` (`date`,`weight`,`totalIntake`,`totalBurned`,`netCalories`,`totalCarbs`,`totalProtein`,`totalFat`,`totalWater`,`sleepDuration`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyRecordEntity entity) {
        statement.bindString(1, entity.getDate());
        if (entity.getWeight() == null) {
          statement.bindNull(2);
        } else {
          statement.bindDouble(2, entity.getWeight());
        }
        statement.bindLong(3, entity.getTotalIntake());
        statement.bindLong(4, entity.getTotalBurned());
        statement.bindLong(5, entity.getNetCalories());
        statement.bindLong(6, entity.getTotalCarbs());
        statement.bindLong(7, entity.getTotalProtein());
        statement.bindLong(8, entity.getTotalFat());
        statement.bindLong(9, entity.getTotalWater());
        statement.bindLong(10, entity.getSleepDuration());
      }
    };
    this.__insertionAdapterOfCalorieItemEntity = new EntityInsertionAdapter<CalorieItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `calorie_items` (`id`,`date`,`type`,`name`,`calories`,`carbs`,`protein`,`fat`,`time`,`imageUrl`,`notes`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalorieItemEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getName());
        statement.bindLong(5, entity.getCalories());
        statement.bindLong(6, entity.getCarbs());
        statement.bindLong(7, entity.getProtein());
        statement.bindLong(8, entity.getFat());
        statement.bindString(9, entity.getTime());
        if (entity.getImageUrl() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getImageUrl());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
        statement.bindString(12, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfCalorieItemEntity = new EntityDeletionOrUpdateAdapter<CalorieItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `calorie_items` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CalorieItemEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfDailyRecordEntity = new EntityDeletionOrUpdateAdapter<DailyRecordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `daily_records` SET `date` = ?,`weight` = ?,`totalIntake` = ?,`totalBurned` = ?,`netCalories` = ?,`totalCarbs` = ?,`totalProtein` = ?,`totalFat` = ?,`totalWater` = ?,`sleepDuration` = ? WHERE `date` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyRecordEntity entity) {
        statement.bindString(1, entity.getDate());
        if (entity.getWeight() == null) {
          statement.bindNull(2);
        } else {
          statement.bindDouble(2, entity.getWeight());
        }
        statement.bindLong(3, entity.getTotalIntake());
        statement.bindLong(4, entity.getTotalBurned());
        statement.bindLong(5, entity.getNetCalories());
        statement.bindLong(6, entity.getTotalCarbs());
        statement.bindLong(7, entity.getTotalProtein());
        statement.bindLong(8, entity.getTotalFat());
        statement.bindLong(9, entity.getTotalWater());
        statement.bindLong(10, entity.getSleepDuration());
        statement.bindString(11, entity.getDate());
      }
    };
    this.__preparedStmtOfDeleteItemById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM calorie_items WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertDailyRecord(final DailyRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyRecordEntity.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDailyRecords(final List<DailyRecordEntity> records,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyRecordEntity_1.insert(records);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertItem(final CalorieItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCalorieItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCalorieItems(final List<CalorieItemEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCalorieItemEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteItem(final CalorieItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCalorieItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDailyRecord(final DailyRecordEntity record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDailyRecordEntity.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteItemById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteItemById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteItemById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<DailyRecordEntity> getDailyRecord(final String date) {
    final String _sql = "SELECT * FROM daily_records WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_records"}, new Callable<DailyRecordEntity>() {
      @Override
      @Nullable
      public DailyRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTotalIntake = CursorUtil.getColumnIndexOrThrow(_cursor, "totalIntake");
          final int _cursorIndexOfTotalBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBurned");
          final int _cursorIndexOfNetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "netCalories");
          final int _cursorIndexOfTotalCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCarbs");
          final int _cursorIndexOfTotalProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "totalProtein");
          final int _cursorIndexOfTotalFat = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFat");
          final int _cursorIndexOfTotalWater = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWater");
          final int _cursorIndexOfSleepDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepDuration");
          final DailyRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final int _tmpTotalIntake;
            _tmpTotalIntake = _cursor.getInt(_cursorIndexOfTotalIntake);
            final int _tmpTotalBurned;
            _tmpTotalBurned = _cursor.getInt(_cursorIndexOfTotalBurned);
            final int _tmpNetCalories;
            _tmpNetCalories = _cursor.getInt(_cursorIndexOfNetCalories);
            final int _tmpTotalCarbs;
            _tmpTotalCarbs = _cursor.getInt(_cursorIndexOfTotalCarbs);
            final int _tmpTotalProtein;
            _tmpTotalProtein = _cursor.getInt(_cursorIndexOfTotalProtein);
            final int _tmpTotalFat;
            _tmpTotalFat = _cursor.getInt(_cursorIndexOfTotalFat);
            final int _tmpTotalWater;
            _tmpTotalWater = _cursor.getInt(_cursorIndexOfTotalWater);
            final int _tmpSleepDuration;
            _tmpSleepDuration = _cursor.getInt(_cursorIndexOfSleepDuration);
            _result = new DailyRecordEntity(_tmpDate,_tmpWeight,_tmpTotalIntake,_tmpTotalBurned,_tmpNetCalories,_tmpTotalCarbs,_tmpTotalProtein,_tmpTotalFat,_tmpTotalWater,_tmpSleepDuration);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDailyRecordSync(final String date,
      final Continuation<? super DailyRecordEntity> $completion) {
    final String _sql = "SELECT * FROM daily_records WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailyRecordEntity>() {
      @Override
      @Nullable
      public DailyRecordEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTotalIntake = CursorUtil.getColumnIndexOrThrow(_cursor, "totalIntake");
          final int _cursorIndexOfTotalBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBurned");
          final int _cursorIndexOfNetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "netCalories");
          final int _cursorIndexOfTotalCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCarbs");
          final int _cursorIndexOfTotalProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "totalProtein");
          final int _cursorIndexOfTotalFat = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFat");
          final int _cursorIndexOfTotalWater = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWater");
          final int _cursorIndexOfSleepDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepDuration");
          final DailyRecordEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final int _tmpTotalIntake;
            _tmpTotalIntake = _cursor.getInt(_cursorIndexOfTotalIntake);
            final int _tmpTotalBurned;
            _tmpTotalBurned = _cursor.getInt(_cursorIndexOfTotalBurned);
            final int _tmpNetCalories;
            _tmpNetCalories = _cursor.getInt(_cursorIndexOfNetCalories);
            final int _tmpTotalCarbs;
            _tmpTotalCarbs = _cursor.getInt(_cursorIndexOfTotalCarbs);
            final int _tmpTotalProtein;
            _tmpTotalProtein = _cursor.getInt(_cursorIndexOfTotalProtein);
            final int _tmpTotalFat;
            _tmpTotalFat = _cursor.getInt(_cursorIndexOfTotalFat);
            final int _tmpTotalWater;
            _tmpTotalWater = _cursor.getInt(_cursorIndexOfTotalWater);
            final int _tmpSleepDuration;
            _tmpSleepDuration = _cursor.getInt(_cursorIndexOfSleepDuration);
            _result = new DailyRecordEntity(_tmpDate,_tmpWeight,_tmpTotalIntake,_tmpTotalBurned,_tmpNetCalories,_tmpTotalCarbs,_tmpTotalProtein,_tmpTotalFat,_tmpTotalWater,_tmpSleepDuration);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CalorieItemEntity>> getItemsForDate(final String date) {
    final String _sql = "SELECT * FROM calorie_items WHERE date = ? ORDER BY time DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calorie_items"}, new Callable<List<CalorieItemEntity>>() {
      @Override
      @NonNull
      public List<CalorieItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs");
          final int _cursorIndexOfProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "protein");
          final int _cursorIndexOfFat = CursorUtil.getColumnIndexOrThrow(_cursor, "fat");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CalorieItemEntity> _result = new ArrayList<CalorieItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalorieItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpCalories;
            _tmpCalories = _cursor.getInt(_cursorIndexOfCalories);
            final int _tmpCarbs;
            _tmpCarbs = _cursor.getInt(_cursorIndexOfCarbs);
            final int _tmpProtein;
            _tmpProtein = _cursor.getInt(_cursorIndexOfProtein);
            final int _tmpFat;
            _tmpFat = _cursor.getInt(_cursorIndexOfFat);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new CalorieItemEntity(_tmpId,_tmpDate,_tmpType,_tmpName,_tmpCalories,_tmpCarbs,_tmpProtein,_tmpFat,_tmpTime,_tmpImageUrl,_tmpNotes,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DailyRecordEntity>> getAllRecords() {
    final String _sql = "SELECT * FROM daily_records ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_records"}, new Callable<List<DailyRecordEntity>>() {
      @Override
      @NonNull
      public List<DailyRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTotalIntake = CursorUtil.getColumnIndexOrThrow(_cursor, "totalIntake");
          final int _cursorIndexOfTotalBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBurned");
          final int _cursorIndexOfNetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "netCalories");
          final int _cursorIndexOfTotalCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCarbs");
          final int _cursorIndexOfTotalProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "totalProtein");
          final int _cursorIndexOfTotalFat = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFat");
          final int _cursorIndexOfTotalWater = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWater");
          final int _cursorIndexOfSleepDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepDuration");
          final List<DailyRecordEntity> _result = new ArrayList<DailyRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyRecordEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final int _tmpTotalIntake;
            _tmpTotalIntake = _cursor.getInt(_cursorIndexOfTotalIntake);
            final int _tmpTotalBurned;
            _tmpTotalBurned = _cursor.getInt(_cursorIndexOfTotalBurned);
            final int _tmpNetCalories;
            _tmpNetCalories = _cursor.getInt(_cursorIndexOfNetCalories);
            final int _tmpTotalCarbs;
            _tmpTotalCarbs = _cursor.getInt(_cursorIndexOfTotalCarbs);
            final int _tmpTotalProtein;
            _tmpTotalProtein = _cursor.getInt(_cursorIndexOfTotalProtein);
            final int _tmpTotalFat;
            _tmpTotalFat = _cursor.getInt(_cursorIndexOfTotalFat);
            final int _tmpTotalWater;
            _tmpTotalWater = _cursor.getInt(_cursorIndexOfTotalWater);
            final int _tmpSleepDuration;
            _tmpSleepDuration = _cursor.getInt(_cursorIndexOfSleepDuration);
            _item = new DailyRecordEntity(_tmpDate,_tmpWeight,_tmpTotalIntake,_tmpTotalBurned,_tmpNetCalories,_tmpTotalCarbs,_tmpTotalProtein,_tmpTotalFat,_tmpTotalWater,_tmpSleepDuration);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllRecordsSync(final Continuation<? super List<DailyRecordEntity>> $completion) {
    final String _sql = "SELECT * FROM daily_records";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyRecordEntity>>() {
      @Override
      @NonNull
      public List<DailyRecordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTotalIntake = CursorUtil.getColumnIndexOrThrow(_cursor, "totalIntake");
          final int _cursorIndexOfTotalBurned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBurned");
          final int _cursorIndexOfNetCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "netCalories");
          final int _cursorIndexOfTotalCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCarbs");
          final int _cursorIndexOfTotalProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "totalProtein");
          final int _cursorIndexOfTotalFat = CursorUtil.getColumnIndexOrThrow(_cursor, "totalFat");
          final int _cursorIndexOfTotalWater = CursorUtil.getColumnIndexOrThrow(_cursor, "totalWater");
          final int _cursorIndexOfSleepDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepDuration");
          final List<DailyRecordEntity> _result = new ArrayList<DailyRecordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyRecordEntity _item;
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final int _tmpTotalIntake;
            _tmpTotalIntake = _cursor.getInt(_cursorIndexOfTotalIntake);
            final int _tmpTotalBurned;
            _tmpTotalBurned = _cursor.getInt(_cursorIndexOfTotalBurned);
            final int _tmpNetCalories;
            _tmpNetCalories = _cursor.getInt(_cursorIndexOfNetCalories);
            final int _tmpTotalCarbs;
            _tmpTotalCarbs = _cursor.getInt(_cursorIndexOfTotalCarbs);
            final int _tmpTotalProtein;
            _tmpTotalProtein = _cursor.getInt(_cursorIndexOfTotalProtein);
            final int _tmpTotalFat;
            _tmpTotalFat = _cursor.getInt(_cursorIndexOfTotalFat);
            final int _tmpTotalWater;
            _tmpTotalWater = _cursor.getInt(_cursorIndexOfTotalWater);
            final int _tmpSleepDuration;
            _tmpSleepDuration = _cursor.getInt(_cursorIndexOfSleepDuration);
            _item = new DailyRecordEntity(_tmpDate,_tmpWeight,_tmpTotalIntake,_tmpTotalBurned,_tmpNetCalories,_tmpTotalCarbs,_tmpTotalProtein,_tmpTotalFat,_tmpTotalWater,_tmpSleepDuration);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CalorieItemEntity>> getAllCalorieItems() {
    final String _sql = "SELECT * FROM calorie_items ORDER BY date DESC, time DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"calorie_items"}, new Callable<List<CalorieItemEntity>>() {
      @Override
      @NonNull
      public List<CalorieItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs");
          final int _cursorIndexOfProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "protein");
          final int _cursorIndexOfFat = CursorUtil.getColumnIndexOrThrow(_cursor, "fat");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CalorieItemEntity> _result = new ArrayList<CalorieItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalorieItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpCalories;
            _tmpCalories = _cursor.getInt(_cursorIndexOfCalories);
            final int _tmpCarbs;
            _tmpCarbs = _cursor.getInt(_cursorIndexOfCarbs);
            final int _tmpProtein;
            _tmpProtein = _cursor.getInt(_cursorIndexOfProtein);
            final int _tmpFat;
            _tmpFat = _cursor.getInt(_cursorIndexOfFat);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new CalorieItemEntity(_tmpId,_tmpDate,_tmpType,_tmpName,_tmpCalories,_tmpCarbs,_tmpProtein,_tmpFat,_tmpTime,_tmpImageUrl,_tmpNotes,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllCalorieItemsSync(
      final Continuation<? super List<CalorieItemEntity>> $completion) {
    final String _sql = "SELECT * FROM calorie_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CalorieItemEntity>>() {
      @Override
      @NonNull
      public List<CalorieItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCalories = CursorUtil.getColumnIndexOrThrow(_cursor, "calories");
          final int _cursorIndexOfCarbs = CursorUtil.getColumnIndexOrThrow(_cursor, "carbs");
          final int _cursorIndexOfProtein = CursorUtil.getColumnIndexOrThrow(_cursor, "protein");
          final int _cursorIndexOfFat = CursorUtil.getColumnIndexOrThrow(_cursor, "fat");
          final int _cursorIndexOfTime = CursorUtil.getColumnIndexOrThrow(_cursor, "time");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CalorieItemEntity> _result = new ArrayList<CalorieItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CalorieItemEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpCalories;
            _tmpCalories = _cursor.getInt(_cursorIndexOfCalories);
            final int _tmpCarbs;
            _tmpCarbs = _cursor.getInt(_cursorIndexOfCarbs);
            final int _tmpProtein;
            _tmpProtein = _cursor.getInt(_cursorIndexOfProtein);
            final int _tmpFat;
            _tmpFat = _cursor.getInt(_cursorIndexOfFat);
            final String _tmpTime;
            _tmpTime = _cursor.getString(_cursorIndexOfTime);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new CalorieItemEntity(_tmpId,_tmpDate,_tmpType,_tmpName,_tmpCalories,_tmpCarbs,_tmpProtein,_tmpFat,_tmpTime,_tmpImageUrl,_tmpNotes,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
