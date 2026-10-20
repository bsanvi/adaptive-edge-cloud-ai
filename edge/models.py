from pydantic import BaseModel


class EdgeTaskRequest(BaseModel):
    """
    Request sent by the Orchestrator when it wants
    the Edge device to resize an image locally.
    """

    task_id: str
    input_path: str
    output_path: str
    width: int
    height: int