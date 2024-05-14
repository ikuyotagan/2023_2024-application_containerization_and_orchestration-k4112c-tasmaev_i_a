package ru.itmo.ict.todolist.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.itmo.ict.todolist.generated.model.Task
import ru.itmo.ict.todolist.AbstractIntegrationTest
import ru.itmo.ict.todolist.Converters
import ru.itmo.ict.todolist.IntegrationTest
import ru.itmo.ict.todolist.generated.jooq.tables.daos.TasksDao
import ru.itmo.ict.todolist.generated.jooq.tables.pojos.Tasks

@IntegrationTest
class TasksControllerTest: AbstractIntegrationTest() {

    @Autowired
    private lateinit var tasksDao: TasksDao

    @Autowired
    private lateinit var converters: Converters

    private lateinit var tasksController: TasksController

    @BeforeEach
    fun setup() {
        tasksController = TasksController(tasksDao, converters)
        tasksDao.findAll().forEach { tasksDao.delete(it) }
    }

    @Test
    fun `listTasks returns all tasks`() {
        val tasks = listOf(Tasks(1, "taska 1"), Tasks(2, "taska 2"))
        tasksDao.insert(tasks)

        val response = tasksController.listTasks()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(tasks.map { converters.convertTask(it) } , response.body)
    }


    @Test
    fun `listTasks returns empty list when no tasks`() {
        val response = tasksController.listTasks()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(emptyList<Task>(), response.body)
    }

    @Test
    fun `getTask returns not found for non-existent task`() {
        val response = tasksController.getTask("9999")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `createTask returns created task with new id`() {
        val task = Task(id = "1", title = "New Task")
        val response = tasksController.createTask(task)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body?.id)
        assertEquals(task.title, response.body?.title)
    }

    @Test
    fun `updateTask returns updated task with same id`() {
        tasksDao.insert(Tasks(id = 1, title = "New Task"))

        val task = Task(id = "1", title = "Updated Task")
        val response = tasksController.updateTask("1", task)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(task.id, response.body?.id)
        assertEquals(task.title, response.body?.title)
    }

    @Test
    fun `deleteTask returns ok for existing task`() {
        val task = Tasks(id = 1, title = "New Task")
        tasksDao.insert(task)

        val response = tasksController.deleteTask("1")

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `deleteTask returns not found for non-existent task`() {
        val response = tasksController.deleteTask("9999")

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}