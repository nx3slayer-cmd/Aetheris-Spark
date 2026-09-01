package com.kallistocore.ai.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.FtsTableInfo;
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
public final class KallistoDatabase_Impl extends KallistoDatabase {
  private volatile MemoryBankDao _memoryBankDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `conversation_sessions` (`sessionId` TEXT NOT NULL, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `modelUsed` TEXT NOT NULL, PRIMARY KEY(`sessionId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_sessions_updatedAt` ON `conversation_sessions` (`updatedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `audioFilePath` TEXT, `imageFilePath` TEXT, `toolDataJson` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_timestamp` ON `chat_messages` (`timestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `memory_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `content` TEXT NOT NULL, `importance` REAL NOT NULL, `timestamp` INTEGER NOT NULL, `sourceSessionId` TEXT, `embeddingVector` TEXT)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_entries_key` ON `memory_entries` (`key`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_entries_timestamp` ON `memory_entries` (`timestamp`)");
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `memory_entries_fts` USING FTS4(`key` TEXT NOT NULL, `content` TEXT NOT NULL, content=`memory_entries`)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_BEFORE_UPDATE BEFORE UPDATE ON `memory_entries` BEGIN DELETE FROM `memory_entries_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_BEFORE_DELETE BEFORE DELETE ON `memory_entries` BEGIN DELETE FROM `memory_entries_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_AFTER_UPDATE AFTER UPDATE ON `memory_entries` BEGIN INSERT INTO `memory_entries_fts`(`docid`, `key`, `content`) VALUES (NEW.`rowid`, NEW.`key`, NEW.`content`); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_AFTER_INSERT AFTER INSERT ON `memory_entries` BEGIN INSERT INTO `memory_entries_fts`(`docid`, `key`, `content`) VALUES (NEW.`rowid`, NEW.`key`, NEW.`content`); END");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'efaf269303de73874bf0c3da8f0ccbc9')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `conversation_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `chat_messages`");
        db.execSQL("DROP TABLE IF EXISTS `memory_entries`");
        db.execSQL("DROP TABLE IF EXISTS `memory_entries_fts`");
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
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_BEFORE_UPDATE BEFORE UPDATE ON `memory_entries` BEGIN DELETE FROM `memory_entries_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_BEFORE_DELETE BEFORE DELETE ON `memory_entries` BEGIN DELETE FROM `memory_entries_fts` WHERE `docid`=OLD.`rowid`; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_AFTER_UPDATE AFTER UPDATE ON `memory_entries` BEGIN INSERT INTO `memory_entries_fts`(`docid`, `key`, `content`) VALUES (NEW.`rowid`, NEW.`key`, NEW.`content`); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_entries_fts_AFTER_INSERT AFTER INSERT ON `memory_entries` BEGIN INSERT INTO `memory_entries_fts`(`docid`, `key`, `content`) VALUES (NEW.`rowid`, NEW.`key`, NEW.`content`); END");
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsConversationSessions = new HashMap<String, TableInfo.Column>(5);
        _columnsConversationSessions.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversationSessions.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversationSessions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversationSessions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsConversationSessions.put("modelUsed", new TableInfo.Column("modelUsed", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysConversationSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesConversationSessions = new HashSet<TableInfo.Index>(1);
        _indicesConversationSessions.add(new TableInfo.Index("index_conversation_sessions_updatedAt", false, Arrays.asList("updatedAt"), Arrays.asList("ASC")));
        final TableInfo _infoConversationSessions = new TableInfo("conversation_sessions", _columnsConversationSessions, _foreignKeysConversationSessions, _indicesConversationSessions);
        final TableInfo _existingConversationSessions = TableInfo.read(db, "conversation_sessions");
        if (!_infoConversationSessions.equals(_existingConversationSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "conversation_sessions(com.kallistocore.ai.data.db.ConversationSessionEntity).\n"
                  + " Expected:\n" + _infoConversationSessions + "\n"
                  + " Found:\n" + _existingConversationSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsChatMessages = new HashMap<String, TableInfo.Column>(8);
        _columnsChatMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("audioFilePath", new TableInfo.Column("audioFilePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("imageFilePath", new TableInfo.Column("imageFilePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChatMessages.put("toolDataJson", new TableInfo.Column("toolDataJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChatMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChatMessages = new HashSet<TableInfo.Index>(2);
        _indicesChatMessages.add(new TableInfo.Index("index_chat_messages_sessionId", false, Arrays.asList("sessionId"), Arrays.asList("ASC")));
        _indicesChatMessages.add(new TableInfo.Index("index_chat_messages_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoChatMessages = new TableInfo("chat_messages", _columnsChatMessages, _foreignKeysChatMessages, _indicesChatMessages);
        final TableInfo _existingChatMessages = TableInfo.read(db, "chat_messages");
        if (!_infoChatMessages.equals(_existingChatMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "chat_messages(com.kallistocore.ai.data.db.MessageEntity).\n"
                  + " Expected:\n" + _infoChatMessages + "\n"
                  + " Found:\n" + _existingChatMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsMemoryEntries = new HashMap<String, TableInfo.Column>(7);
        _columnsMemoryEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("key", new TableInfo.Column("key", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("importance", new TableInfo.Column("importance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("sourceSessionId", new TableInfo.Column("sourceSessionId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMemoryEntries.put("embeddingVector", new TableInfo.Column("embeddingVector", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMemoryEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMemoryEntries = new HashSet<TableInfo.Index>(2);
        _indicesMemoryEntries.add(new TableInfo.Index("index_memory_entries_key", false, Arrays.asList("key"), Arrays.asList("ASC")));
        _indicesMemoryEntries.add(new TableInfo.Index("index_memory_entries_timestamp", false, Arrays.asList("timestamp"), Arrays.asList("ASC")));
        final TableInfo _infoMemoryEntries = new TableInfo("memory_entries", _columnsMemoryEntries, _foreignKeysMemoryEntries, _indicesMemoryEntries);
        final TableInfo _existingMemoryEntries = TableInfo.read(db, "memory_entries");
        if (!_infoMemoryEntries.equals(_existingMemoryEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "memory_entries(com.kallistocore.ai.data.db.MemoryEntryEntity).\n"
                  + " Expected:\n" + _infoMemoryEntries + "\n"
                  + " Found:\n" + _existingMemoryEntries);
        }
        final HashSet<String> _columnsMemoryEntriesFts = new HashSet<String>(3);
        _columnsMemoryEntriesFts.add("key");
        _columnsMemoryEntriesFts.add("content");
        final FtsTableInfo _infoMemoryEntriesFts = new FtsTableInfo("memory_entries_fts", _columnsMemoryEntriesFts, "CREATE VIRTUAL TABLE IF NOT EXISTS `memory_entries_fts` USING FTS4(`key` TEXT NOT NULL, `content` TEXT NOT NULL, content=`memory_entries`)");
        final FtsTableInfo _existingMemoryEntriesFts = FtsTableInfo.read(db, "memory_entries_fts");
        if (!_infoMemoryEntriesFts.equals(_existingMemoryEntriesFts)) {
          return new RoomOpenHelper.ValidationResult(false, "memory_entries_fts(com.kallistocore.ai.data.db.MemoryFtsEntity).\n"
                  + " Expected:\n" + _infoMemoryEntriesFts + "\n"
                  + " Found:\n" + _existingMemoryEntriesFts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "efaf269303de73874bf0c3da8f0ccbc9", "6554e724d2f581323b9393df2a7b42f6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(1);
    _shadowTablesMap.put("memory_entries_fts", "memory_entries");
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "conversation_sessions","chat_messages","memory_entries","memory_entries_fts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `conversation_sessions`");
      _db.execSQL("DELETE FROM `chat_messages`");
      _db.execSQL("DELETE FROM `memory_entries`");
      _db.execSQL("DELETE FROM `memory_entries_fts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(MemoryBankDao.class, MemoryBankDao_Impl.getRequiredConverters());
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
  public MemoryBankDao memoryBankDao() {
    if (_memoryBankDao != null) {
      return _memoryBankDao;
    } else {
      synchronized(this) {
        if(_memoryBankDao == null) {
          _memoryBankDao = new MemoryBankDao_Impl(this);
        }
        return _memoryBankDao;
      }
    }
  }
}
