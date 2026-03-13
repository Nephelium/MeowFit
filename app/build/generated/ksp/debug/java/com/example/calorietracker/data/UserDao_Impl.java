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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserDao_Impl implements UserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProfileEntity> __insertionAdapterOfUserProfileEntity;

  private final EntityDeletionOrUpdateAdapter<UserProfileEntity> __updateAdapterOfUserProfileEntity;

  public UserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProfileEntity = new EntityInsertionAdapter<UserProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_profile` (`id`,`name`,`gender`,`age`,`birthDate`,`height`,`weight`,`targetWeight`,`activityLevel`,`goal`,`dailyCalorieTarget`,`sleepGoal`,`showMacros`,`excludedExercises`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfileEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getGender());
        statement.bindLong(4, entity.getAge());
        statement.bindString(5, entity.getBirthDate());
        statement.bindDouble(6, entity.getHeight());
        statement.bindDouble(7, entity.getWeight());
        statement.bindDouble(8, entity.getTargetWeight());
        statement.bindString(9, entity.getActivityLevel());
        statement.bindString(10, entity.getGoal());
        statement.bindLong(11, entity.getDailyCalorieTarget());
        statement.bindDouble(12, entity.getSleepGoal());
        final int _tmp = entity.getShowMacros() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindString(14, entity.getExcludedExercises());
        statement.bindString(15, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfUserProfileEntity = new EntityDeletionOrUpdateAdapter<UserProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `user_profile` SET `id` = ?,`name` = ?,`gender` = ?,`age` = ?,`birthDate` = ?,`height` = ?,`weight` = ?,`targetWeight` = ?,`activityLevel` = ?,`goal` = ?,`dailyCalorieTarget` = ?,`sleepGoal` = ?,`showMacros` = ?,`excludedExercises` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfileEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getGender());
        statement.bindLong(4, entity.getAge());
        statement.bindString(5, entity.getBirthDate());
        statement.bindDouble(6, entity.getHeight());
        statement.bindDouble(7, entity.getWeight());
        statement.bindDouble(8, entity.getTargetWeight());
        statement.bindString(9, entity.getActivityLevel());
        statement.bindString(10, entity.getGoal());
        statement.bindLong(11, entity.getDailyCalorieTarget());
        statement.bindDouble(12, entity.getSleepGoal());
        final int _tmp = entity.getShowMacros() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindString(14, entity.getExcludedExercises());
        statement.bindString(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getId());
      }
    };
  }

  @Override
  public Object insertUserProfile(final UserProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateUserProfile(final UserProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserProfileEntity.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserProfileEntity> getUserProfile() {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_profile"}, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfBirthDate = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDate");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTargetWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeight");
          final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfDailyCalorieTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyCalorieTarget");
          final int _cursorIndexOfSleepGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepGoal");
          final int _cursorIndexOfShowMacros = CursorUtil.getColumnIndexOrThrow(_cursor, "showMacros");
          final int _cursorIndexOfExcludedExercises = CursorUtil.getColumnIndexOrThrow(_cursor, "excludedExercises");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGender;
            _tmpGender = _cursor.getString(_cursorIndexOfGender);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpBirthDate;
            _tmpBirthDate = _cursor.getString(_cursorIndexOfBirthDate);
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final float _tmpWeight;
            _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            final float _tmpTargetWeight;
            _tmpTargetWeight = _cursor.getFloat(_cursorIndexOfTargetWeight);
            final String _tmpActivityLevel;
            _tmpActivityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final int _tmpDailyCalorieTarget;
            _tmpDailyCalorieTarget = _cursor.getInt(_cursorIndexOfDailyCalorieTarget);
            final float _tmpSleepGoal;
            _tmpSleepGoal = _cursor.getFloat(_cursorIndexOfSleepGoal);
            final boolean _tmpShowMacros;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfShowMacros);
            _tmpShowMacros = _tmp != 0;
            final String _tmpExcludedExercises;
            _tmpExcludedExercises = _cursor.getString(_cursorIndexOfExcludedExercises);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpName,_tmpGender,_tmpAge,_tmpBirthDate,_tmpHeight,_tmpWeight,_tmpTargetWeight,_tmpActivityLevel,_tmpGoal,_tmpDailyCalorieTarget,_tmpSleepGoal,_tmpShowMacros,_tmpExcludedExercises,_tmpCreatedAt);
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
  public Object getUserProfileSync(final Continuation<? super UserProfileEntity> $completion) {
    final String _sql = "SELECT * FROM user_profile WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfBirthDate = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDate");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfTargetWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeight");
          final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
          final int _cursorIndexOfGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "goal");
          final int _cursorIndexOfDailyCalorieTarget = CursorUtil.getColumnIndexOrThrow(_cursor, "dailyCalorieTarget");
          final int _cursorIndexOfSleepGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "sleepGoal");
          final int _cursorIndexOfShowMacros = CursorUtil.getColumnIndexOrThrow(_cursor, "showMacros");
          final int _cursorIndexOfExcludedExercises = CursorUtil.getColumnIndexOrThrow(_cursor, "excludedExercises");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpGender;
            _tmpGender = _cursor.getString(_cursorIndexOfGender);
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpBirthDate;
            _tmpBirthDate = _cursor.getString(_cursorIndexOfBirthDate);
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final float _tmpWeight;
            _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            final float _tmpTargetWeight;
            _tmpTargetWeight = _cursor.getFloat(_cursorIndexOfTargetWeight);
            final String _tmpActivityLevel;
            _tmpActivityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
            final String _tmpGoal;
            _tmpGoal = _cursor.getString(_cursorIndexOfGoal);
            final int _tmpDailyCalorieTarget;
            _tmpDailyCalorieTarget = _cursor.getInt(_cursorIndexOfDailyCalorieTarget);
            final float _tmpSleepGoal;
            _tmpSleepGoal = _cursor.getFloat(_cursorIndexOfSleepGoal);
            final boolean _tmpShowMacros;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfShowMacros);
            _tmpShowMacros = _tmp != 0;
            final String _tmpExcludedExercises;
            _tmpExcludedExercises = _cursor.getString(_cursorIndexOfExcludedExercises);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpName,_tmpGender,_tmpAge,_tmpBirthDate,_tmpHeight,_tmpWeight,_tmpTargetWeight,_tmpActivityLevel,_tmpGoal,_tmpDailyCalorieTarget,_tmpSleepGoal,_tmpShowMacros,_tmpExcludedExercises,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
