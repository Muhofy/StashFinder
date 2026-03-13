#!/usr/bin/env python3
import typer
import subprocess
from rich.console import Console
from rich.panel import Panel
from InquirerPy import inquirer

app = typer.Typer(help="G-CLI: Modern Git Assistant")
console = Console()

def run_git(args: list, silent=False):
    try:
        result = subprocess.run(["git"] + args, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        if not silent:
            console.print(f"[bold red]Git Error:[/bold red] {e.stderr}")
        return None

def show_status():
    out = run_git(["status", "-s"])
    if not out:
        console.print("[bold green]✔ Working tree clean.[/bold green]")
    else:
        console.print(Panel(out, title="[bold blue]Local Changes[/bold blue]", border_style="blue"))

def handle_branches():
    branches_raw = run_git(["branch", "--format=%(refname:short)"])
    if branches_raw is None: return
    branches = branches_raw.split('\n')
    current = run_git(["branch", "--show-current"])
    
    target = inquirer.select(
        message=f"Current branch: {current}. Select target:",
        choices=branches + ["+ Create New Branch", "<< Back"],
    ).execute()

    if target == "<< Back": return
    if target == "+ Create New Branch":
        name = inquirer.text(message="New branch name:").execute()
        if name: 
            run_git(["checkout", "-b", name])
            console.print(f"[bold green]Created and switched to {name}[/bold green]")
    else:
        run_git(["checkout", target])
        console.print(f"[bold green]Switched to {target}[/bold green]")

@app.callback(invoke_without_command=True)
def main(ctx: typer.Context):
    if ctx.invoked_subcommand is not None:
        return

    while True:
        console.print(Panel.fit("[bold magenta]G-CLI TERMINAL[/bold magenta]", border_style="magenta"))
        
        choice = inquirer.select(
            message="Action:",
            choices=[
                "Status", 
                "Add + Commit (Local)", 
                "Sync (Add+Commit+Push)", 
                "Pull", 
                "Branch Manager", 
                "Recent Logs", 
                "Exit"
            ],
            cycle=True
        ).execute()

        if choice == "Status": 
            show_status()
        
        elif choice == "Add + Commit (Local)":
            msg = inquirer.text(message="Commit message:").execute()
            if msg:
                run_git(["add", "."])
                run_git(["commit", "-m", msg])
                console.print(f"[bold green]✔ Committed locally: {msg}[/bold green]")

        elif choice == "Sync (Add+Commit+Push)":
            msg = inquirer.text(message="Commit message:").execute()
            if msg:
                run_git(["add", "."])
                run_git(["commit", "-m", msg])
                with console.status("[bold yellow]Pushing to remote...[/bold yellow]"):
                    run_git(["push"])
                console.print("[bold green]✔ Deployed successfully to remote.[/bold green]")

        elif choice == "Pull":
            with console.status("[bold cyan]Pulling changes...[/bold cyan]"):
                run_git(["pull"])
            console.print("[bold cyan]✔ Synced with remote.[/bold cyan]")

        elif choice == "Branch Manager": 
            handle_branches()

        elif choice == "Recent Logs":
            log = run_git(["log", "--oneline", "-n", "10", "--graph", "--color=always"])
            if log:
                console.print(Panel(log, title="Git Graph"))

        elif choice == "Exit":
            console.print("[yellow]Keep coding! Goodbye.[/yellow]")
            break

if __name__ == "__main__":
    app()

