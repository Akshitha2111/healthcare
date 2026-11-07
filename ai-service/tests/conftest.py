import pytest

from app import create_app
from predict import Predictor


@pytest.fixture(scope="session")
def predictor():
    return Predictor.load("model")


@pytest.fixture()
def client(predictor):
    return create_app(predictor=predictor).test_client()
