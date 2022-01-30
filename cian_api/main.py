from fastapi import FastAPI
import asyncio
import pickle
import random


with open('result.pkl', 'rb') as f:
    data = pickle.load(f)

app = FastAPI()

@app.get('/query')
async def query(metro: str, rooms: int, pricelow: int, pricehigh: int):
    return [flat for flat in data if 
                flat['metro_search'].lower() == metro.lower() and
                flat['rooms_search'] == rooms and
                pricelow < flat['price'] < pricehigh
            ]
    

@app.get("/random")
async def random_query():
    return random.choice(data)

