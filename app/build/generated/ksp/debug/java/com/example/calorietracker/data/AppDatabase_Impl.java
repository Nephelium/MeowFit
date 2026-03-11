package com.example.calorietracker.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile UserDao _userDao;

  private volatile RecordDao _recordDao;

  private volatile AiDao _aiDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `gender` TEXT NOT NULL, `age` INTEGER NOT NULL, `height` REAL NOT NULL, `weight` REAL NOT NULL, `targetWeight` REAL NOT NULL, `activityLevel` TEXT NOT NULL, `goal` TEXT NOT NULL, `dailyCalorieTarget` INTEGER NOT NULL, `sleepGoal` REAL NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_records` (`date` TEXT NOT NULL, `weight` REAL, `totalIntake` INTEGER NOT NULL, `totalBurned` INTEGER NOT NULL, `netCalories` INTEGER NOT NULL, `totalWater` INTEGER NOT NULL, `sleepDuration` INTEGER NOT NULL, PRIMARY KEY(`date`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `calorie_items` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `type` TEXT NOT NULL, `name` TEXT NOT NULL, `calories` INTEGER NOT NULL, `time` TEXT NOT NULL, `imageUrl` TEXT, `notes` TEXT, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`date`) REFERENCES `daily_records`(`date`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calorie_items_date` ON `calorie_items` (`date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `imageUrl` TEXT, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e9c2f64e3180decc7cc70eca66fcccbf')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_profile`");
        db.execSQL("DROP TABLE IF EXISTS `daily_records`");
        db.execSQL("DROP TABLE IF EXISTS `calorie_items`");
        db.execSQL("DROP TABLE IF EXISTS `ai_chat_messages`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserProfile = new HashMap<String, TableInfo.Column>(12);
        _columnsUserProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("gender", new TableInfo.Column("gender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("age", new TableInfo.Column("age", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("height", new TableInfo.Column("height", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("weight", new TableInfo.Column("weight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("targetWeight", new TableInfo.Column("targetWeight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("activityLevel", new TableInfo.Column("activityLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("goal", new TableInfo.Column("goal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("dailyCalorieTarget", new TableInfo.Column("dailyCalorieTarget", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("sleepGoal", new TableInfo.Column("sleepGoal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProfile = new TableInfo("user_profile", _columnsUserProfile, _foreignKeysUserProfile, _indicesUserProfile);
        final TableInfo _existingUserProfile = TableInfo.read(db, "user_profile");
        if (!_infoUserProfile.equals(_existingUserProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profile(com.example.calorietracker.data.UserProfileEntity).\n"
                  + " Expected:\n" + _infoUserProfile + "\n"
                  + " Found:\n" + _existingUserProfile);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyRecords = new HashMap<String, TableInfo.Column>(7);
        _columnsDailyRecords.put("date", new TableInfo.Column("date", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("weight", new TableInfo.Column("weight", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("totalIntake", new TableInfo.Column("totalIntake", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("totalBurned", new TableInfo.Column("totalBurned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("netCalories", new TableInfo.Column("netCalories", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("totalWater", new TableInfo.Column("totalWater", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyRecords.put("sleepDuration", new TableInfo.Column("sleepDuration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDailyRecords = new TableInfo("daily_records", _columnsDailyRecords, _foreignKeysDailyRecords, _indicesDailyRecords);
        final TableInfo _existingDailyRecords = TableInfo.read(db, "daily_records");
        if (!_infoDailyRecords.equals(_existingDailyRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_records(com.example.calorietracker.data.DailyRecordEntity).\n"
                  + " Expected:\n" + _infoDailyRecords + "\n"
                  + " Found:\n" + _existingDailyRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsCalorieItems = new HashMap<String, TableInfo.Column>(9);
        _columnsCalorieItems.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("calories", new TableInfo.Column("calories", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("time", new TableInfo.Column("time", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCalorieItems.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCalorieItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCalorieItems.add(new TableInfo.ForeignKey("daily_records", "CASCADE", "NO ACTION", Arrays.asList("date"), Arrays.asList("date")));
        final HashSet<TableInfo.Index> _indicesCalorieItems = new HashSet<TableInfo.Index>(1);
        _indicesCalorieItems.add(new TableInfo.Index("index_calorie_items_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        final TableInfo _infoCalorieItems = new TableInfo("calorie_items", _columnsCalorieItems, _foreignKeysCalorieItems, _indicesCalorieItems);
        final TableInfo _existingCalorieItems = TableInfo.read(db, "calorie_items");
        if (!_infoCalorieItems.equals(_existingCalorieItems)) {
          return new RoomOpenHelper.ValidationResult(false, "calorie_items(com.example.calorietracker.data.CalorieItemEntity).\n"
                  + " Expected:\n" + _infoCalorieItems + "\n"
                  + " Found:\n" + _existingCalorieItems);
        }
        final HashMap<String, TableInfo.Column> _columnsAiChatMessages = new HashMap<String, TableInfo.Column>(5);
        _columnsAiChatMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiChatMessages.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiChatMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiChatMessages.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiChatMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAiChatMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAiChatMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAiChatMessages = new TableInfo("ai_chat_messages", _columnsAiChatMessages, _foreignKeysAiChatMessages, _indicesAiChatMessages);
        final TableInfo _existingAiChatMessages = TableInfo.read(db, "ai_chat_messages");
        if (!_infoAiChatMessages.equals(_existingAiChatMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "ai_chat_messages(com.example.calorietracker.data.AiChatMessageEntity).\n"
                  + " Expected:\n" + _infoAiChatMessages + "\n"
                  + " Found:\n" + _existingAiChatMessages);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e9c2f64e3180decc7cc70eca66fcccbf", "de29eac877d36b47ce2f5f8f47065f85");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_profile","daily_records","calorie_items","ai_chat_messages");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `user_profile`");
      _db.execSQL("DELETE FROM `daily_records`");
      _db.execSQL("DELETE FROM `calorie_items`");
      _db.execSQL("DELETE FROM `ai_chat_messages`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RecordDao.class, RecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AiDao.class, AiDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public RecordDao recordDao() {
    if (_recordDao != null) {
      return _recordDao;
    } else {
      synchronized(this) {
        if(_recordDao == null) {
          _recordDao = new RecordDao_Impl(this);
        }
        return _recordDao;
      }
    }
  }

  @Override
  public AiDao aiDao() {
    if (_aiDao != null) {
      return _aiDao;
    } else {
      synchronized(this) {
        if(_aiDao == null) {
          _aiDao = new AiDao_Impl(this);
        }
        return _aiDao;
      }
    }
  }
}
