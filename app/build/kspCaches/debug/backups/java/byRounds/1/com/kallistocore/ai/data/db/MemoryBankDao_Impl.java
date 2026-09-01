package com.kallistocore.ai.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class MemoryBankDao_Impl implements MemoryBankDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ConversationSessionEntity> __insertionAdapterOfConversationSessionEntity;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final EntityInsertionAdapter<MemoryEntryEntity> __insertionAdapterOfMemoryEntryEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSessionTimestamp;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMessagesBySession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteMemory;

  private final SharedSQLiteStatement __preparedStmtOfClearAllMemories;

  private final SharedSQLiteStatement __preparedStmtOfPruneOldestLowPriorityMemories;

  public MemoryBankDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfConversationSessionEntity = new EntityInsertionAdapter<ConversationSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `conversation_sessions` (`sessionId`,`title`,`createdAt`,`updatedAt`,`modelUsed`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ConversationSessionEntity entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getCreatedAt());
        statement.bindLong(4, entity.getUpdatedAt());
        statement.bindString(5, entity.getModelUsed());
      }
    };
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chat_messages` (`id`,`sessionId`,`role`,`content`,`timestamp`,`audioFilePath`,`imageFilePath`,`toolDataJson`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindString(3, entity.getRole());
        statement.bindString(4, entity.getContent());
        statement.bindLong(5, entity.getTimestamp());
        if (entity.getAudioFilePath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getAudioFilePath());
        }
        if (entity.getImageFilePath() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getImageFilePath());
        }
        if (entity.getToolDataJson() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getToolDataJson());
        }
      }
    };
    this.__insertionAdapterOfMemoryEntryEntity = new EntityInsertionAdapter<MemoryEntryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `memory_entries` (`id`,`key`,`content`,`importance`,`timestamp`,`sourceSessionId`,`embeddingVector`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MemoryEntryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getKey());
        statement.bindString(3, entity.getContent());
        statement.bindDouble(4, entity.getImportance());
        statement.bindLong(5, entity.getTimestamp());
        if (entity.getSourceSessionId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getSourceSessionId());
        }
        if (entity.getEmbeddingVector() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEmbeddingVector());
        }
      }
    };
    this.__preparedStmtOfUpdateSessionTimestamp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE conversation_sessions SET updatedAt = ? WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM conversation_sessions WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteMessagesBySession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chat_messages WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteMemory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM memory_entries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllMemories = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM memory_entries";
        return _query;
      }
    };
    this.__preparedStmtOfPruneOldestLowPriorityMemories = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        DELETE FROM memory_entries \n"
                + "        WHERE id IN (\n"
                + "            SELECT id FROM memory_entries \n"
                + "            ORDER BY importance ASC, timestamp ASC \n"
                + "            LIMIT ?\n"
                + "        )\n"
                + "    ";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final ConversationSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfConversationSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMessage(final MessageEntity message,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMemory(final MemoryEntryEntity memory,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMemoryEntryEntity.insertAndReturnId(memory);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSessionTimestamp(final String sessionId, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSessionTimestamp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfUpdateSessionTimestamp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfDeleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMessagesBySession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMessagesBySession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfDeleteMessagesBySession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMemory(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteMemory.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteMemory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllMemories(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllMemories.acquire();
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
          __preparedStmtOfClearAllMemories.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneOldestLowPriorityMemories(final int count,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneOldestLowPriorityMemories.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, count);
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
          __preparedStmtOfPruneOldestLowPriorityMemories.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ConversationSessionEntity>> getAllSessions() {
    final String _sql = "SELECT * FROM conversation_sessions ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"conversation_sessions"}, new Callable<List<ConversationSessionEntity>>() {
      @Override
      @NonNull
      public List<ConversationSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfModelUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "modelUsed");
          final List<ConversationSessionEntity> _result = new ArrayList<ConversationSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ConversationSessionEntity _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final String _tmpModelUsed;
            _tmpModelUsed = _cursor.getString(_cursorIndexOfModelUsed);
            _item = new ConversationSessionEntity(_tmpSessionId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt,_tmpModelUsed);
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
  public Flow<List<MessageEntity>> getMessagesForSession(final String sessionId) {
    final String _sql = "SELECT * FROM chat_messages WHERE sessionId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chat_messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAudioFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFilePath");
          final int _cursorIndexOfImageFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imageFilePath");
          final int _cursorIndexOfToolDataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "toolDataJson");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpAudioFilePath;
            if (_cursor.isNull(_cursorIndexOfAudioFilePath)) {
              _tmpAudioFilePath = null;
            } else {
              _tmpAudioFilePath = _cursor.getString(_cursorIndexOfAudioFilePath);
            }
            final String _tmpImageFilePath;
            if (_cursor.isNull(_cursorIndexOfImageFilePath)) {
              _tmpImageFilePath = null;
            } else {
              _tmpImageFilePath = _cursor.getString(_cursorIndexOfImageFilePath);
            }
            final String _tmpToolDataJson;
            if (_cursor.isNull(_cursorIndexOfToolDataJson)) {
              _tmpToolDataJson = null;
            } else {
              _tmpToolDataJson = _cursor.getString(_cursorIndexOfToolDataJson);
            }
            _item = new MessageEntity(_tmpId,_tmpSessionId,_tmpRole,_tmpContent,_tmpTimestamp,_tmpAudioFilePath,_tmpImageFilePath,_tmpToolDataJson);
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
  public Object getRecentMessages(final String sessionId, final int limit,
      final Continuation<? super List<MessageEntity>> $completion) {
    final String _sql = "SELECT * FROM chat_messages WHERE sessionId = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAudioFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFilePath");
          final int _cursorIndexOfImageFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imageFilePath");
          final int _cursorIndexOfToolDataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "toolDataJson");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpAudioFilePath;
            if (_cursor.isNull(_cursorIndexOfAudioFilePath)) {
              _tmpAudioFilePath = null;
            } else {
              _tmpAudioFilePath = _cursor.getString(_cursorIndexOfAudioFilePath);
            }
            final String _tmpImageFilePath;
            if (_cursor.isNull(_cursorIndexOfImageFilePath)) {
              _tmpImageFilePath = null;
            } else {
              _tmpImageFilePath = _cursor.getString(_cursorIndexOfImageFilePath);
            }
            final String _tmpToolDataJson;
            if (_cursor.isNull(_cursorIndexOfToolDataJson)) {
              _tmpToolDataJson = null;
            } else {
              _tmpToolDataJson = _cursor.getString(_cursorIndexOfToolDataJson);
            }
            _item = new MessageEntity(_tmpId,_tmpSessionId,_tmpRole,_tmpContent,_tmpTimestamp,_tmpAudioFilePath,_tmpImageFilePath,_tmpToolDataJson);
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
  public Object getTotalMessageCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM chat_messages";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<List<MemoryEntryEntity>> getAllMemories() {
    final String _sql = "SELECT * FROM memory_entries ORDER BY importance DESC, timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"memory_entries"}, new Callable<List<MemoryEntryEntity>>() {
      @Override
      @NonNull
      public List<MemoryEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfImportance = CursorUtil.getColumnIndexOrThrow(_cursor, "importance");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSourceSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceSessionId");
          final int _cursorIndexOfEmbeddingVector = CursorUtil.getColumnIndexOrThrow(_cursor, "embeddingVector");
          final List<MemoryEntryEntity> _result = new ArrayList<MemoryEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemoryEntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final float _tmpImportance;
            _tmpImportance = _cursor.getFloat(_cursorIndexOfImportance);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpSourceSessionId;
            if (_cursor.isNull(_cursorIndexOfSourceSessionId)) {
              _tmpSourceSessionId = null;
            } else {
              _tmpSourceSessionId = _cursor.getString(_cursorIndexOfSourceSessionId);
            }
            final String _tmpEmbeddingVector;
            if (_cursor.isNull(_cursorIndexOfEmbeddingVector)) {
              _tmpEmbeddingVector = null;
            } else {
              _tmpEmbeddingVector = _cursor.getString(_cursorIndexOfEmbeddingVector);
            }
            _item = new MemoryEntryEntity(_tmpId,_tmpKey,_tmpContent,_tmpImportance,_tmpTimestamp,_tmpSourceSessionId,_tmpEmbeddingVector);
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
  public Object getMemoryByKey(final String key,
      final Continuation<? super MemoryEntryEntity> $completion) {
    final String _sql = "SELECT * FROM memory_entries WHERE `key` = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MemoryEntryEntity>() {
      @Override
      @Nullable
      public MemoryEntryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfImportance = CursorUtil.getColumnIndexOrThrow(_cursor, "importance");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSourceSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceSessionId");
          final int _cursorIndexOfEmbeddingVector = CursorUtil.getColumnIndexOrThrow(_cursor, "embeddingVector");
          final MemoryEntryEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final float _tmpImportance;
            _tmpImportance = _cursor.getFloat(_cursorIndexOfImportance);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpSourceSessionId;
            if (_cursor.isNull(_cursorIndexOfSourceSessionId)) {
              _tmpSourceSessionId = null;
            } else {
              _tmpSourceSessionId = _cursor.getString(_cursorIndexOfSourceSessionId);
            }
            final String _tmpEmbeddingVector;
            if (_cursor.isNull(_cursorIndexOfEmbeddingVector)) {
              _tmpEmbeddingVector = null;
            } else {
              _tmpEmbeddingVector = _cursor.getString(_cursorIndexOfEmbeddingVector);
            }
            _result = new MemoryEntryEntity(_tmpId,_tmpKey,_tmpContent,_tmpImportance,_tmpTimestamp,_tmpSourceSessionId,_tmpEmbeddingVector);
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
  public Object searchMemoriesFts(final String query, final int limit,
      final Continuation<? super List<MemoryEntryEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM memory_entries \n"
            + "        WHERE content LIKE '%' || ? || '%' OR `key` LIKE '%' || ? || '%'\n"
            + "        ORDER BY importance DESC \n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MemoryEntryEntity>>() {
      @Override
      @NonNull
      public List<MemoryEntryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfImportance = CursorUtil.getColumnIndexOrThrow(_cursor, "importance");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSourceSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceSessionId");
          final int _cursorIndexOfEmbeddingVector = CursorUtil.getColumnIndexOrThrow(_cursor, "embeddingVector");
          final List<MemoryEntryEntity> _result = new ArrayList<MemoryEntryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MemoryEntryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final float _tmpImportance;
            _tmpImportance = _cursor.getFloat(_cursorIndexOfImportance);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpSourceSessionId;
            if (_cursor.isNull(_cursorIndexOfSourceSessionId)) {
              _tmpSourceSessionId = null;
            } else {
              _tmpSourceSessionId = _cursor.getString(_cursorIndexOfSourceSessionId);
            }
            final String _tmpEmbeddingVector;
            if (_cursor.isNull(_cursorIndexOfEmbeddingVector)) {
              _tmpEmbeddingVector = null;
            } else {
              _tmpEmbeddingVector = _cursor.getString(_cursorIndexOfEmbeddingVector);
            }
            _item = new MemoryEntryEntity(_tmpId,_tmpKey,_tmpContent,_tmpImportance,_tmpTimestamp,_tmpSourceSessionId,_tmpEmbeddingVector);
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
  public Object getApproximateMemoryBankSizeBytes(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(LENGTH(content)) FROM memory_entries";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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
