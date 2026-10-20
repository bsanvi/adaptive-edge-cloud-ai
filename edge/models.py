from pydantic import BaseModel


class EdgeTaskRequest(BaseModel):
    """
    Data that the Orchestrator sends when it wants
    the Edge device to execute a task.
    """

    task_id: str
    number_count: int