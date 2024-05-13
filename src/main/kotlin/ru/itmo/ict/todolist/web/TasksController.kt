package ru.itmo.ict.todolist.web

import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import ru.itmo.ict.todolist.Converters
import ru.itmo.ict.todolist.generated.api.DefaultApi
import ru.itmo.ict.todolist.generated.jooq.tables.daos.TasksDao
import ru.itmo.ict.todolist.generated.model.Task

@RestController
class TasksController(
    private val tasksDao: TasksDao,
    private val converters: Converters,
) : DefaultApi {
    override fun listTasks(): ResponseEntity<List<Task>> {
        return ResponseEntity.ok().body(
            tasksDao.findAll().map { converters.convertTask(it) },
        )
    }

    override fun getTask(taskId: String): ResponseEntity<Task> {
        return ResponseEntity.ok().body(
            tasksDao.fetchOneById(taskId.toInt())?.let { converters.convertTask(it) },
        )
    }

    @Transactional
    override fun createTask(task: Task): ResponseEntity<Task> {
        tasksDao.fetchOneById(task.id.toInt())?.let { return ResponseEntity.badRequest().build() }
        tasksDao.insert(converters.convertTask(task))
        return ResponseEntity.ok().body(task)
    }

    @Transactional
    override fun updateTask(
        taskId: String,
        task: Task,
    ): ResponseEntity<Task> {
        tasksDao.fetchOneById(task.id.toInt())?.let { return ResponseEntity.badRequest().build() }
        return ResponseEntity.ok().body(
            tasksDao.update(converters.convertTask(task.copy(id = taskId))).let { task },
        )
    }

    override fun deleteTask(taskId: String): ResponseEntity<Unit> {
        return ResponseEntity.ok().body(
            tasksDao.deleteById(taskId.toInt()),
        )
    }
}
