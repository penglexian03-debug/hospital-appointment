import asyncio
import json
import sys
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.agent.tools.catalog import build_catalog_tools
from app.clients.java_api import JavaApiClient
from app.core.config import get_settings


async def main() -> None:
    client = JavaApiClient(get_settings())
    tools = {tool.name: tool for tool in build_catalog_tools(client)}

    departments = await tools["list_departments"].ainvoke({})
    doctors = await tools["search_doctors"].ainvoke({})
    schedules = await tools["query_schedules"].ainvoke({"only_available": True})

    print("departments:")
    print(json.dumps(json.loads(departments), ensure_ascii=False, indent=2))
    print("doctors:")
    print(json.dumps(json.loads(doctors), ensure_ascii=False, indent=2))
    print("schedules:")
    print(json.dumps(json.loads(schedules), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    asyncio.run(main())
