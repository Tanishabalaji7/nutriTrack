from pgmpy.models import BayesianModel
from pgmpy.factors.discrete import TabularCPD
from pgmpy.inference import VariableElimination


# Define structure
model = BayesianModel([
    ('Rain', 'Traffic'),
    ('Accident', 'Traffic')
])


# Define CPDs

cpd_rain = TabularCPD(
    variable='Rain',
    variable_card=2,
    values=[
        [0.7],
        [0.3]
    ]
)

cpd_accident = TabularCPD(
    variable='Accident',
    variable_card=2,
    values=[
        [0.9],
        [0.1]
    ]
)

cpd_traffic = TabularCPD(
    variable='Traffic',
    variable_card=2,
    values=[
        [0.95, 0.8, 0.7, 0.1],
        [0.05, 0.2, 0.3, 0.9]
    ],
    evidence=['Rain', 'Accident'],
    evidence_card=[2, 2]
)


# Add CPDs to the model
model.add_cpds(
    cpd_rain,
    cpd_accident,
    cpd_traffic
)


# Check the model
assert model.check_model()


# Perform inference
inference = VariableElimination(model)


# Query probability of Traffic given Rain = 1 (True)
posterior = inference.query(
    variables=['Traffic'],
    evidence={'Rain': 1}
)


# Display result
print(posterior)