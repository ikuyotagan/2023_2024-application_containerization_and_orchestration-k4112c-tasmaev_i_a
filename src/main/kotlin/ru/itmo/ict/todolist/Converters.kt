package ru.itmo.ict.todolist

import org.springframework.stereotype.Component
import ru.itmo.ict.todolist.generated.jooq.tables.pojos.Tasks
import ru.itmo.ict.todolist.generated.model.Task

@Component
class Converters {
    fun convertTask(task: Task): Tasks {
        return Tasks(
            id = task.id.toInt(),
            title = task.title,
            description = task.description,
            completed = task.completed,
        )
    }

    fun convertTask(taskDto: Tasks): Task {
        return Task(
            id = taskDto.id.toString(),
            title = taskDto.title,
            description = taskDto.description,
            completed = taskDto.completed,
        )
    }
}
