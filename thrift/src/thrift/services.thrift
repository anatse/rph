
enum Operation {
    ADD = 1,
    SUBTRACT = 2,
    MULTIPLY = 3,
    DIVIDE = 4
}

struct Work {
    1: required Operation    op,
    2: optional i32          status
}

service WorkService {
    list<Operation> findOperationList()

}